package hsf302.bookingsystemcinema.repository;

import hsf302.bookingsystemcinema.entity.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByBookingId(Long bookingId);

    /**
     * Check if any active ticket exists for a given showtime + seat combination.
     * Used during Hard Lock validation before creating a new Booking.
     */
    @Query("SELECT t FROM Ticket t WHERE t.showtime.id = :showtimeId AND t.seat.id IN :seatIds")
    List<Ticket> findActiveTickets(@Param("showtimeId") Long showtimeId,
                                   @Param("seatIds") List<Long> seatIds);

    /**
     * Pessimistic Lock version - locks the Seat rows involved so no concurrent
     * transaction can INSERT a ticket for the same showtime+seat pair simultaneously.
     * Called inside @Transactional during the "Create Booking" flow.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.showtime.id = :showtimeId AND t.seat.id IN :seatIds")
    List<Ticket> findTicketsForUpdate(@Param("showtimeId") Long showtimeId,
                                      @Param("seatIds") List<Long> seatIds);
}
