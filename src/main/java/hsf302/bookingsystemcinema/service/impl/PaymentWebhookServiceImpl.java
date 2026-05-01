package hsf302.bookingsystemcinema.service.impl;

import hsf302.bookingsystemcinema.dto.PaymentWebhookRequest;
import hsf302.bookingsystemcinema.entity.Booking;
import hsf302.bookingsystemcinema.entity.PaymentTransaction;
import hsf302.bookingsystemcinema.entity.Ticket;
import hsf302.bookingsystemcinema.entity.enums.BookingStatus;
import hsf302.bookingsystemcinema.entity.enums.PaymentMethod;
import hsf302.bookingsystemcinema.entity.enums.PaymentStatus;
import hsf302.bookingsystemcinema.exception.ResourceNotFoundException;
import hsf302.bookingsystemcinema.repository.BookingRepository;
import hsf302.bookingsystemcinema.repository.PaymentTransactionRepository;
import hsf302.bookingsystemcinema.repository.TicketRepository;
import hsf302.bookingsystemcinema.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final BookingRepository bookingRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public String processWebhook(PaymentWebhookRequest request) {
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║  [Webhook] Received callback for Booking #{}        ║", request.getBookingId());
        log.info("║  TransactionId: {}  |  Status: {}                   ║", request.getTransactionId(), request.getStatus());
        log.info("╚══════════════════════════════════════════════════════╝");

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        PaymentStatus incomingStatus;
        try {
            incomingStatus = PaymentStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid payment status: " + request.getStatus()
                    + ". Expected: SUCCESS or FAILED");
        }

        // ═══════════════════════════════════════════════════════
        // CASE 1: SUCCESS + Booking is HOLDING → Normal flow
        // ═══════════════════════════════════════════════════════
        if (incomingStatus == PaymentStatus.SUCCESS && booking.getStatus() == BookingStatus.HOLDING) {
            return handleSuccessfulPayment(booking, request.getTransactionId());
        }

        // ═══════════════════════════════════════════════════════
        // CASE 2: SUCCESS + Booking is CANCELLED → Late payment!
        // ═══════════════════════════════════════════════════════
        if (incomingStatus == PaymentStatus.SUCCESS && booking.getStatus() == BookingStatus.CANCELLED) {
            return handleLatePayment(booking, request.getTransactionId());
        }

        // ═══════════════════════════════════════════════════════
        // CASE 3: SUCCESS + Booking is PAID → Duplicate webhook
        // ═══════════════════════════════════════════════════════
        if (incomingStatus == PaymentStatus.SUCCESS && booking.getStatus() == BookingStatus.PAID) {
            log.warn("  [Webhook] Duplicate SUCCESS webhook for Booking #{} (already PAID). Ignored.",
                    booking.getId());
            return "DUPLICATE_IGNORED";
        }

        // ═══════════════════════════════════════════════════════
        // CASE 4: FAILED → Cancel booking if still HOLDING
        // ═══════════════════════════════════════════════════════
        if (incomingStatus == PaymentStatus.FAILED) {
            return handleFailedPayment(booking, request.getTransactionId());
        }

        log.warn("  [Webhook] Unhandled scenario: Booking #{} status={}, webhook status={}",
                booking.getId(), booking.getStatus(), incomingStatus);
        return "UNHANDLED";
    }

    // ────────────────────────────────────────────────────────
    // CASE 1: Normal successful payment
    // ────────────────────────────────────────────────────────

    private String handleSuccessfulPayment(Booking booking, String transactionId) {
        // Update existing PENDING transaction or create new one
        PaymentTransaction txn = findOrCreateTransaction(booking, transactionId);
        txn.setStatus(PaymentStatus.SUCCESS);
        txn.setTransactionId(transactionId);
        txn.setPaymentTime(LocalDateTime.now());
        paymentTransactionRepository.save(txn);

        // Finalize booking
        booking.setStatus(BookingStatus.PAID);
        booking.setBookingCode(generateBookingCode());
        bookingRepository.save(booking);

        // Mark tickets as not checked in
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        tickets.forEach(t -> t.setIsCheckedIn(false));
        ticketRepository.saveAll(tickets);

        log.info("  ✅ [Webhook] Booking #{} → PAID. Code: {}", booking.getId(), booking.getBookingCode());
        return "PAID";
    }

    // ────────────────────────────────────────────────────────
    // CASE 2: Late payment — Scheduler already cancelled
    // ────────────────────────────────────────────────────────

    private String handleLatePayment(Booking booking, String transactionId) {
        log.error("╔═══════════════════════════════════════════════════════════════╗");
        log.error("║  ⚠️ [LATE PAYMENT] Booking #{} was CANCELLED by Scheduler    ║", booking.getId());
        log.error("║  but payment gateway returned SUCCESS (txn: {})              ║", transactionId);
        log.error("║  Action: Creating NEEDS_REFUND transaction for manual review ║");
        log.error("╚═══════════════════════════════════════════════════════════════╝");

        // Record the late payment as NEEDS_REFUND
        PaymentTransaction refundTxn = PaymentTransaction.builder()
                .booking(booking)
                .paymentMethod(PaymentMethod.MOCK)
                .transactionId(transactionId)
                .amount(booking.getTotalAmount())
                .paymentTime(LocalDateTime.now())
                .status(PaymentStatus.NEEDS_REFUND)
                .build();
        paymentTransactionRepository.save(refundTxn);

        log.error("  → PaymentTransaction #{} created with status NEEDS_REFUND. "
                + "Customer support must process refund manually.", refundTxn.getId());

        return "NEEDS_REFUND";
    }

    // ────────────────────────────────────────────────────────
    // CASE 4: Failed payment
    // ────────────────────────────────────────────────────────

    private String handleFailedPayment(Booking booking, String transactionId) {
        PaymentTransaction txn = findOrCreateTransaction(booking, transactionId);
        txn.setStatus(PaymentStatus.FAILED);
        txn.setTransactionId(transactionId);
        txn.setPaymentTime(LocalDateTime.now());
        paymentTransactionRepository.save(txn);

        if (booking.getStatus() == BookingStatus.HOLDING) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            log.info("  ❌ [Webhook] Payment FAILED for Booking #{}. Status → CANCELLED", booking.getId());
        } else {
            log.info("  ❌ [Webhook] Payment FAILED for Booking #{} (already {}). Transaction recorded.",
                    booking.getId(), booking.getStatus());
        }

        return "FAILED";
    }

    // ────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────

    private PaymentTransaction findOrCreateTransaction(Booking booking, String transactionId) {
        return paymentTransactionRepository.findByTransactionId(transactionId)
                .orElse(PaymentTransaction.builder()
                        .booking(booking)
                        .paymentMethod(PaymentMethod.MOCK)
                        .amount(booking.getTotalAmount())
                        .status(PaymentStatus.PENDING)
                        .build());
    }

    private String generateBookingCode() {
        String prefix = "VNP";
        String random = String.valueOf(100000 + new Random().nextInt(900000));
        return prefix + random;
    }
}
