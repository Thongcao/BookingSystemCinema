package hsf302.bookingsystemcinema.scheduler;

import hsf302.bookingsystemcinema.entity.Booking;
import hsf302.bookingsystemcinema.entity.Ticket;
import hsf302.bookingsystemcinema.entity.enums.BookingStatus;
import hsf302.bookingsystemcinema.repository.BookingRepository;
import hsf302.bookingsystemcinema.repository.TicketRepository;
import hsf302.bookingsystemcinema.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCleanupScheduler {

    private static final int HOLDING_TIMEOUT_MINUTES = 15;
    private static final String REDIS_KEY_PREFIX = "booking:showtime:%d:seat:%d";

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final VoucherRepository voucherRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Runs every minute. Finds all HOLDING bookings older than 15 minutes,
     * cancels them, deletes their tickets (frees the Hard Lock), and
     * restores voucher usage if applicable.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void cleanupExpiredBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(HOLDING_TIMEOUT_MINUTES);

        List<Booking> expiredBookings = bookingRepository
                .findByStatusAndBookingTimeBefore(BookingStatus.HOLDING, cutoff);

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("╔══════════════════════════════════════════════════╗");
        log.info("║  [Scheduler] Found {} expired HOLDING booking(s) ║", expiredBookings.size());
        log.info("╚══════════════════════════════════════════════════╝");

        int totalTicketsDeleted = 0;
        int totalVouchersRestored = 0;

        for (Booking booking : expiredBookings) {
            Long bookingId = booking.getId();

            // ─── 1. Delete Tickets (free Hard Lock) ────────────────
            List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
            if (!tickets.isEmpty()) {
                // Clean Redis soft-locks for these seats (safety net)
                Long showtimeId = booking.getShowtime().getId();
                for (Ticket ticket : tickets) {
                    String key = String.format(REDIS_KEY_PREFIX, showtimeId, ticket.getSeat().getId());
                    redisTemplate.delete(key);
                }

                ticketRepository.deleteAll(tickets);
                totalTicketsDeleted += tickets.size();
                log.info("  → Booking #{}: Deleted {} ticket(s), freed DB Hard Lock",
                        bookingId, tickets.size());
            }

            // ─── 2. Restore Voucher usage ──────────────────────────
            if (booking.getVoucherCode() != null && !booking.getVoucherCode().isBlank()) {
                voucherRepository.findByCode(booking.getVoucherCode()).ifPresent(voucher -> {
                    voucher.setUsageLimit(voucher.getUsageLimit() + 1);
                    voucherRepository.save(voucher);
                    log.info("  → Booking #{}: Restored voucher '{}' usage (+1 → {})",
                            bookingId, voucher.getCode(), voucher.getUsageLimit());
                });
                totalVouchersRestored++;
            }

            // ─── 3. Cancel Booking ─────────────────────────────────
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            log.info("  → Booking #{}: Status changed HOLDING → CANCELLED", bookingId);
        }

        log.info("═══ [Scheduler] Cleanup complete: {} booking(s) cancelled, {} ticket(s) deleted, {} voucher(s) restored ═══",
                expiredBookings.size(), totalTicketsDeleted, totalVouchersRestored);
    }
}
