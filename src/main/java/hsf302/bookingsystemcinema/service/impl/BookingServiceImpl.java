package hsf302.bookingsystemcinema.service.impl;

import hsf302.bookingsystemcinema.dto.*;
import hsf302.bookingsystemcinema.entity.*;
import hsf302.bookingsystemcinema.entity.enums.*;
import hsf302.bookingsystemcinema.exception.DoubleBookingException;
import hsf302.bookingsystemcinema.exception.ResourceNotFoundException;
import hsf302.bookingsystemcinema.repository.*;
import hsf302.bookingsystemcinema.service.BookingService;
import hsf302.bookingsystemcinema.service.payment.MockPaymentStrategy;
import hsf302.bookingsystemcinema.service.payment.PaymentResult;
import hsf302.bookingsystemcinema.service.payment.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final BigDecimal VIP_SURCHARGE = new BigDecimal("30000");
    private static final BigDecimal SWEETBOX_SURCHARGE = new BigDecimal("50000");
    private static final String REDIS_KEY_PREFIX = "booking:showtime:%d:seat:%d";

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final VoucherRepository voucherRepository;

    private final PaymentStrategy paymentStrategy;
    private final RedisTemplate<String, String> redisTemplate;

    // ════════════════════════════════════════════════════════
    // CREATE BOOKING — Main Checkout Flow
    // ════════════════════════════════════════════════════════

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, Long authenticatedUserId) {
        log.info(">>> [Checkout] Starting booking for user={} showtime={} seats={}",
                authenticatedUserId, request.getShowtimeId(), request.getSeats().size());

        // ────────────────────────────────────────
        // STEP 1: Validate entities
        // ────────────────────────────────────────
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authenticatedUserId));

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", request.getShowtimeId()));

        List<Long> seatIds = request.getSeats().stream()
                .map(SeatBookingRequest::getSeatId)
                .collect(Collectors.toList());

        Map<Long, Seat> seatMap = loadAndValidateSeats(seatIds, showtime.getRoom().getId());

        // ────────────────────────────────────────
        // STEP 2: PESSIMISTIC LOCK — Hard Lock check
        // ────────────────────────────────────────
        List<Ticket> conflicting = ticketRepository.findTicketsForUpdate(showtime.getId(), seatIds);
        if (!conflicting.isEmpty()) {
            Long conflictSeatId = conflicting.get(0).getSeat().getId();
            throw new DoubleBookingException(showtime.getId(), conflictSeatId);
        }

        // ────────────────────────────────────────
        // STEP 3: Calculate pricing
        // ────────────────────────────────────────
        BigDecimal ticketSubtotal = calculateTicketSubtotal(request.getSeats(), seatMap, showtime.getBasePrice());
        BigDecimal itemSubtotal = calculateItemSubtotal(request.getItems());
        BigDecimal grandTotal = ticketSubtotal.add(itemSubtotal);

        // ────────────────────────────────────────
        // STEP 4: Apply Voucher (if provided)
        // ────────────────────────────────────────
        Voucher voucher = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        String voucherCode = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucher = validateAndGetVoucher(request.getVoucherCode(), grandTotal);
            voucherCode = voucher.getCode();
            discountAmount = calculateDiscount(voucher, grandTotal);
            grandTotal = grandTotal.subtract(discountAmount);
            if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
                grandTotal = BigDecimal.ZERO;
            }
        }

        // ────────────────────────────────────────
        // STEP 5: Create Booking entity (status = HOLDING)
        // ────────────────────────────────────────
        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.HOLDING)
                .totalAmount(grandTotal)
                .voucherCode(voucherCode)
                .discountAmount(discountAmount)
                .build();
        booking = bookingRepository.save(booking);
        log.info(">>> [Checkout] Booking #{} created (HOLDING)", booking.getId());

        // ────────────────────────────────────────
        // STEP 6: Create BookingDetails (F&B items)
        // ────────────────────────────────────────
        List<BookingDetail> bookingDetails = createBookingDetails(booking, request.getItems());

        // ────────────────────────────────────────
        // STEP 7: Create Tickets (Hard Lock via UniqueConstraint)
        // ────────────────────────────────────────
        List<Ticket> tickets = createTickets(booking, showtime, request.getSeats(), seatMap);

        // ────────────────────────────────────────
        // STEP 8: Clean up Redis soft-locks
        // ────────────────────────────────────────
        cleanRedisLocks(showtime.getId(), seatIds);

        // ────────────────────────────────────────
        // STEP 9: Decrement voucher usage
        // ────────────────────────────────────────
        if (voucher != null) {
            voucher.setUsageLimit(voucher.getUsageLimit() - 1);
            voucherRepository.save(voucher);
        }

        // ────────────────────────────────────────
        // STEP 10: Process Payment
        // ────────────────────────────────────────
        PaymentTransaction paymentTxn = createPaymentTransaction(booking);
        PaymentResult paymentResult = paymentStrategy.processPayment(booking);

        paymentTxn.setTransactionId(paymentResult.getTransactionId());
        paymentTxn.setPaymentTime(LocalDateTime.now());
        paymentTxn.setStatus(paymentResult.getStatus());
        paymentTransactionRepository.save(paymentTxn);

        // ────────────────────────────────────────
        // STEP 11: Finalize based on payment result
        // ────────────────────────────────────────
        if (paymentResult.getStatus() == PaymentStatus.SUCCESS) {
            booking.setStatus(BookingStatus.PAID);
            booking.setBookingCode(generateBookingCode());

            tickets.forEach(t -> t.setIsCheckedIn(false));
            ticketRepository.saveAll(tickets);

            bookingRepository.save(booking);
            log.info(">>> [Checkout] Booking #{} PAID. Code: {}", booking.getId(), booking.getBookingCode());
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            log.warn(">>> [Checkout] Payment FAILED for Booking #{}. Status -> CANCELLED", booking.getId());
        }

        return buildBookingResponse(booking, tickets, bookingDetails, paymentTxn);
    }

    // ════════════════════════════════════════════════════════
    // GET BOOKING BY ID
    // ════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        List<BookingDetail> details = booking.getBookingDetails();
        PaymentTransaction txn = booking.getPaymentTransactions() != null && !booking.getPaymentTransactions().isEmpty()
                ? booking.getPaymentTransactions().get(0) : null;

        return buildBookingResponse(booking, tickets, details, txn);
    }

    // ════════════════════════════════════════════════════════
    // PRIVATE: Validation Helpers
    // ════════════════════════════════════════════════════════

    private Map<Long, Seat> loadAndValidateSeats(List<Long> seatIds, Long roomId) {
        List<Seat> seats = seatRepository.findAllById(seatIds);

        if (seats.size() != seatIds.size()) {
            Set<Long> foundIds = seats.stream().map(Seat::getId).collect(Collectors.toSet());
            Long missingId = seatIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new ResourceNotFoundException("Seat", "id", missingId);
        }

        for (Seat seat : seats) {
            if (!seat.getRoom().getId().equals(roomId)) {
                throw new IllegalArgumentException(
                        String.format("Seat %d does not belong to the showtime's room (room %d)", seat.getId(), roomId));
            }
            if (seat.getStatus() == SeatStatus.MAINTENANCE) {
                throw new IllegalArgumentException(
                        String.format("Seat %d is under maintenance and cannot be booked", seat.getId()));
            }
        }

        return seats.stream().collect(Collectors.toMap(Seat::getId, s -> s));
    }

    private Voucher validateAndGetVoucher(String code, BigDecimal orderTotal) {
        Voucher voucher = voucherRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "code", code));

        if (voucher.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Voucher '" + code + "' has expired");
        }
        if (voucher.getUsageLimit() <= 0) {
            throw new IllegalArgumentException("Voucher '" + code + "' has reached its usage limit");
        }

        return voucher;
    }

    // ════════════════════════════════════════════════════════
    // PRIVATE: Pricing Calculation
    // ════════════════════════════════════════════════════════

    private BigDecimal calculateTicketSubtotal(List<SeatBookingRequest> seatRequests,
                                               Map<Long, Seat> seatMap,
                                               BigDecimal basePrice) {
        BigDecimal total = BigDecimal.ZERO;
        for (SeatBookingRequest req : seatRequests) {
            Seat seat = seatMap.get(req.getSeatId());
            BigDecimal surcharge = getSurcharge(seat.getType());
            BigDecimal ticketPrice = basePrice.add(surcharge);
            total = total.add(ticketPrice);
        }
        return total;
    }

    private BigDecimal calculateItemSubtotal(List<ItemBookingRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ItemBookingRequest req : itemRequests) {
            Item item = itemRepository.findById(req.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item", "id", req.getItemId()));
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(req.getQuantity())));
        }
        return total;
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal grandTotal) {
        BigDecimal rawDiscount = grandTotal
                .multiply(voucher.getDiscountPercent())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);

        if (rawDiscount.compareTo(voucher.getMaxDiscount()) > 0) {
            return voucher.getMaxDiscount();
        }
        return rawDiscount;
    }

    private BigDecimal getSurcharge(SeatType type) {
        return switch (type) {
            case VIP -> VIP_SURCHARGE;
            case SWEETBOX -> SWEETBOX_SURCHARGE;
            default -> BigDecimal.ZERO;
        };
    }

    // ════════════════════════════════════════════════════════
    // PRIVATE: Entity Creation
    // ════════════════════════════════════════════════════════

    private List<BookingDetail> createBookingDetails(Booking booking, List<ItemBookingRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return Collections.emptyList();
        }

        List<BookingDetail> details = new ArrayList<>();
        for (ItemBookingRequest req : itemRequests) {
            Item item = itemRepository.findById(req.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item", "id", req.getItemId()));

            BookingDetail detail = BookingDetail.builder()
                    .booking(booking)
                    .item(item)
                    .quantity(req.getQuantity())
                    .unitPrice(item.getPrice())
                    .build();
            details.add(detail);
        }
        return bookingDetailRepository.saveAll(details);
    }

    private List<Ticket> createTickets(Booking booking, Showtime showtime,
                                       List<SeatBookingRequest> seatRequests, Map<Long, Seat> seatMap) {
        List<Ticket> tickets = new ArrayList<>();

        for (SeatBookingRequest req : seatRequests) {
            Seat seat = seatMap.get(req.getSeatId());
            TicketType ticketType = parseTicketType(req.getTicketType());
            BigDecimal ticketPrice = showtime.getBasePrice().add(getSurcharge(seat.getType()));

            Ticket ticket = Ticket.builder()
                    .booking(booking)
                    .seat(seat)
                    .showtime(showtime)
                    .price(ticketPrice)
                    .ticketType(ticketType)
                    .isCheckedIn(false)
                    .build();
            tickets.add(ticket);
        }

        try {
            return ticketRepository.saveAll(tickets);
        } catch (DataIntegrityViolationException ex) {
            log.error(">>> [Hard Lock] DataIntegrityViolation — double booking detected! Rolling back...");
            throw new DoubleBookingException(
                    "Double booking detected for showtime " + showtime.getId() +
                    ". Another user has already booked one of the selected seats. Transaction rolled back.");
        }
    }

    private PaymentTransaction createPaymentTransaction(Booking booking) {
        PaymentTransaction txn = PaymentTransaction.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.MOCK)
                .amount(booking.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .build();
        return paymentTransactionRepository.save(txn);
    }

    // ════════════════════════════════════════════════════════
    // PRIVATE: Redis Cleanup & Utilities
    // ════════════════════════════════════════════════════════

    private void cleanRedisLocks(Long showtimeId, List<Long> seatIds) {
        for (Long seatId : seatIds) {
            String key = String.format(REDIS_KEY_PREFIX, showtimeId, seatId);
            redisTemplate.delete(key);
        }
        log.info(">>> [Redis] Cleaned {} soft-lock keys for showtime {}", seatIds.size(), showtimeId);
    }

    private TicketType parseTicketType(String type) {
        if (type == null || type.isBlank()) {
            return TicketType.ADULT;
        }
        try {
            return TicketType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TicketType.ADULT;
        }
    }

    private String generateBookingCode() {
        String prefix = "VNP";
        String random = String.valueOf(100000 + new Random().nextInt(900000));
        return prefix + random;
    }

    // ════════════════════════════════════════════════════════
    // PRIVATE: Response Builder
    // ════════════════════════════════════════════════════════

    private BookingResponse buildBookingResponse(Booking booking, List<Ticket> tickets,
                                                  List<BookingDetail> details, PaymentTransaction txn) {
        List<BookingResponse.TicketInfo> ticketInfos = tickets.stream().map(t ->
                BookingResponse.TicketInfo.builder()
                        .ticketId(t.getId())
                        .seatRow(t.getSeat().getSeatRow())
                        .seatNumber(t.getSeat().getSeatNumber())
                        .seatType(t.getSeat().getType().name())
                        .ticketType(t.getTicketType().name())
                        .price(t.getPrice())
                        .build()
        ).collect(Collectors.toList());

        List<BookingResponse.ItemInfo> itemInfos = Collections.emptyList();
        if (details != null && !details.isEmpty()) {
            itemInfos = details.stream().map(d ->
                    BookingResponse.ItemInfo.builder()
                            .itemName(d.getItem().getName())
                            .quantity(d.getQuantity())
                            .unitPrice(d.getUnitPrice())
                            .lineTotal(d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                            .build()
            ).collect(Collectors.toList());
        }

        BigDecimal ticketSubtotal = tickets.stream()
                .map(Ticket::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal itemSubtotal = BigDecimal.ZERO;
        if (details != null) {
            itemSubtotal = details.stream()
                    .map(d -> d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .status(booking.getStatus().name())
                .movieTitle(booking.getShowtime().getMovie().getTitle())
                .cinemaName(booking.getShowtime().getRoom().getCinema().getName())
                .roomName(booking.getShowtime().getRoom().getName())
                .showStartTime(booking.getShowtime().getStartTime())
                .tickets(ticketInfos)
                .items(itemInfos)
                .ticketSubtotal(ticketSubtotal)
                .itemSubtotal(itemSubtotal)
                .voucherCode(booking.getVoucherCode())
                .discountAmount(booking.getDiscountAmount())
                .totalAmount(booking.getTotalAmount())
                .paymentMethod(txn != null ? txn.getPaymentMethod().name() : null)
                .paymentStatus(txn != null ? txn.getStatus().name() : null)
                .paymentTransactionId(txn != null ? txn.getTransactionId() : null)
                .build();
    }
}
