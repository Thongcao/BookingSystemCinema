package hsf302.bookingsystemcinema.service.impl;

import hsf302.bookingsystemcinema.dto.HoldSeatsResponse;
import hsf302.bookingsystemcinema.dto.SeatMapItem;
import hsf302.bookingsystemcinema.dto.SeatMapResponse;
import hsf302.bookingsystemcinema.entity.Seat;
import hsf302.bookingsystemcinema.entity.Showtime;
import hsf302.bookingsystemcinema.entity.Ticket;
import hsf302.bookingsystemcinema.entity.enums.SeatStatus;
import hsf302.bookingsystemcinema.entity.enums.SeatType;
import hsf302.bookingsystemcinema.exception.ResourceNotFoundException;
import hsf302.bookingsystemcinema.exception.SeatAlreadyLockedException;
import hsf302.bookingsystemcinema.repository.SeatRepository;
import hsf302.bookingsystemcinema.repository.ShowtimeRepository;
import hsf302.bookingsystemcinema.repository.TicketRepository;
import hsf302.bookingsystemcinema.service.SeatSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatSelectionServiceImpl implements SeatSelectionService {

    private static final Duration SOFT_LOCK_TTL = Duration.ofMinutes(5);
    private static final String KEY_PREFIX = "booking:showtime:%d:seat:%d";
    private static final BigDecimal VIP_SURCHARGE = new BigDecimal("30000");
    private static final BigDecimal SWEETBOX_SURCHARGE = new BigDecimal("50000");

    private final RedisTemplate<String, String> redisTemplate;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;

    // ─────────────────────────────────────────────
    // 1. GET SEAT MAP (merge DB + Redis + Tickets)
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SeatMapResponse getSeatMap(Long showtimeId, Long currentUserId) {
        Showtime showtime = findShowtimeOrThrow(showtimeId);
        Long roomId = showtime.getRoom().getId();

        List<Seat> seats = seatRepository.findByRoomIdOrderByGridRowAscGridColumnAsc(roomId);
        if (seats.isEmpty()) {
            throw new ResourceNotFoundException("Seats", "roomId", roomId);
        }

        // Collect all seat IDs to query tickets in one batch
        List<Long> allSeatIds = seats.stream().map(Seat::getId).collect(Collectors.toList());

        // DB Hard-booked seats: tickets that exist for this showtime (HOLDING or PAID)
        List<Ticket> existingTickets = ticketRepository.findActiveTickets(showtimeId, allSeatIds);
        Set<Long> hardBookedSeatIds = existingTickets.stream()
                .map(t -> t.getSeat().getId())
                .collect(Collectors.toSet());

        // Redis Soft-locked seats: scan Redis keys for this showtime
        Map<Long, String> softLockedSeats = getSoftLockedSeats(showtimeId, allSeatIds);

        // Compute grid dimensions
        int totalRows = seats.stream().mapToInt(Seat::getGridRow).max().orElse(0) + 1;
        int totalColumns = seats.stream().mapToInt(Seat::getGridColumn).max().orElse(0) + 1;

        // Merge and build response
        List<SeatMapItem> seatMapItems = seats.stream().map(seat -> {
            String displayStatus = resolveDisplayStatus(seat, hardBookedSeatIds, softLockedSeats);
            BigDecimal surcharge = calculateSurcharge(seat.getType());

            String heldByCurrentUser = "false";
            if (currentUserId != null) {
                String holder = softLockedSeats.get(seat.getId());
                if (holder != null && holder.equals(String.valueOf(currentUserId))) {
                    heldByCurrentUser = "true";
                }
            }

            return SeatMapItem.builder()
                    .seatId(seat.getId())
                    .seatRow(seat.getSeatRow())
                    .seatNumber(seat.getSeatNumber())
                    .gridRow(seat.getGridRow())
                    .gridColumn(seat.getGridColumn())
                    .seatType(seat.getType().name())
                    .displayStatus(displayStatus)
                    .surcharge(surcharge)
                    .heldByCurrentUser(heldByCurrentUser)
                    .build();
        }).collect(Collectors.toList());

        return SeatMapResponse.builder()
                .showtimeId(showtimeId)
                .movieTitle(showtime.getMovie().getTitle())
                .roomName(showtime.getRoom().getName())
                .cinemaName(showtime.getRoom().getCinema().getName())
                .basePrice(showtime.getBasePrice())
                .totalRows(totalRows)
                .totalColumns(totalColumns)
                .seats(seatMapItems)
                .build();
    }

    // ─────────────────────────────────────────────
    // 2. HOLD SEATS (Redis Soft Lock with rollback)
    // ─────────────────────────────────────────────

    @Override
    public HoldSeatsResponse holdSeats(Long showtimeId, Long userId, List<Long> seatIds) {
        findShowtimeOrThrow(showtimeId);

        // Pre-check: seats must exist and belong to the showtime's room
        validateSeatsExist(showtimeId, seatIds);

        // Pre-check: no hard-booked tickets in DB
        List<Ticket> conflictingTickets = ticketRepository.findActiveTickets(showtimeId, seatIds);
        if (!conflictingTickets.isEmpty()) {
            Long conflictSeatId = conflictingTickets.get(0).getSeat().getId();
            throw new SeatAlreadyLockedException(conflictSeatId);
        }

        // Attempt Redis Soft Lock with atomic rollback
        List<Long> successfullyLocked = new ArrayList<>();
        String userIdStr = String.valueOf(userId);

        try {
            for (Long seatId : seatIds) {
                String key = buildKey(showtimeId, seatId);

                // setIfAbsent = SETNX: only sets if key does NOT exist (atomic)
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(key, userIdStr, SOFT_LOCK_TTL);

                if (Boolean.TRUE.equals(acquired)) {
                    successfullyLocked.add(seatId);
                    log.info("Soft-locked seat {} for user {} on showtime {}", seatId, userId, showtimeId);
                } else {
                    // Key already exists — check if same user re-selecting
                    String currentHolder = redisTemplate.opsForValue().get(key);
                    if (userIdStr.equals(currentHolder)) {
                        // Same user re-selecting: refresh TTL
                        redisTemplate.expire(key, SOFT_LOCK_TTL);
                        successfullyLocked.add(seatId);
                        log.info("Refreshed soft-lock for seat {} (same user {})", seatId, userId);
                    } else {
                        // Different user holds this seat -> ROLLBACK and throw
                        throw new SeatAlreadyLockedException(seatId, currentHolder);
                    }
                }
            }
        } catch (SeatAlreadyLockedException ex) {
            // Rollback: delete all keys we just set in this batch
            rollbackLockedSeats(showtimeId, successfullyLocked);
            log.warn("Hold-seats rollback: released {} seats for user {} due to conflict on seat {}",
                    successfullyLocked.size(), userId, ex.getSeatId());
            throw ex;
        }

        return HoldSeatsResponse.builder()
                .showtimeId(showtimeId)
                .userId(userId)
                .heldSeatIds(successfullyLocked)
                .ttlSeconds((int) SOFT_LOCK_TTL.getSeconds())
                .build();
    }

    // ─────────────────────────────────────────────
    // 3. RELEASE SEATS (Manual unlock)
    // ─────────────────────────────────────────────

    @Override
    public void releaseSeats(Long showtimeId, Long userId, List<Long> seatIds) {
        String userIdStr = String.valueOf(userId);

        for (Long seatId : seatIds) {
            String key = buildKey(showtimeId, seatId);
            String currentHolder = redisTemplate.opsForValue().get(key);

            // Only the holder can release their own seats
            if (userIdStr.equals(currentHolder)) {
                redisTemplate.delete(key);
                log.info("Released soft-lock on seat {} for user {} on showtime {}", seatId, userId, showtimeId);
            }
        }
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private String buildKey(Long showtimeId, Long seatId) {
        return String.format(KEY_PREFIX, showtimeId, seatId);
    }

    private Showtime findShowtimeOrThrow(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime", "id", showtimeId));
    }

    private void validateSeatsExist(Long showtimeId, List<Long> seatIds) {
        Showtime showtime = findShowtimeOrThrow(showtimeId);
        Long roomId = showtime.getRoom().getId();

        List<Seat> roomSeats = seatRepository.findByRoomIdOrderByGridRowAscGridColumnAsc(roomId);
        Set<Long> validSeatIds = roomSeats.stream()
                .filter(s -> s.getStatus() == SeatStatus.AVAILABLE)
                .map(Seat::getId)
                .collect(Collectors.toSet());

        for (Long seatId : seatIds) {
            if (!validSeatIds.contains(seatId)) {
                throw new ResourceNotFoundException("Seat", "id (in this room)", seatId);
            }
        }
    }

    private Map<Long, String> getSoftLockedSeats(Long showtimeId, List<Long> seatIds) {
        Map<Long, String> lockedMap = new HashMap<>();
        for (Long seatId : seatIds) {
            String key = buildKey(showtimeId, seatId);
            String holder = redisTemplate.opsForValue().get(key);
            if (holder != null) {
                lockedMap.put(seatId, holder);
            }
        }
        return lockedMap;
    }

    private String resolveDisplayStatus(Seat seat, Set<Long> hardBookedSeatIds, Map<Long, String> softLockedSeats) {
        // Priority 1: Physical seat is under maintenance
        if (seat.getStatus() == SeatStatus.MAINTENANCE) {
            return "MAINTENANCE";
        }
        // Priority 2: Hard-booked in DB (ticket exists with HOLDING or PAID booking)
        if (hardBookedSeatIds.contains(seat.getId())) {
            return "BOOKED";
        }
        // Priority 3: Soft-locked in Redis (someone is selecting)
        if (softLockedSeats.containsKey(seat.getId())) {
            return "HOLDING";
        }
        // Default
        return "AVAILABLE";
    }

    private BigDecimal calculateSurcharge(SeatType seatType) {
        return switch (seatType) {
            case VIP -> VIP_SURCHARGE;
            case SWEETBOX -> SWEETBOX_SURCHARGE;
            default -> BigDecimal.ZERO;
        };
    }

    private void rollbackLockedSeats(Long showtimeId, List<Long> lockedSeatIds) {
        for (Long seatId : lockedSeatIds) {
            String key = buildKey(showtimeId, seatId);
            redisTemplate.delete(key);
        }
    }
}
