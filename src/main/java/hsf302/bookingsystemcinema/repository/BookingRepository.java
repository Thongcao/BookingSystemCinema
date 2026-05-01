package hsf302.bookingsystemcinema.repository;

import hsf302.bookingsystemcinema.entity.Booking;
import hsf302.bookingsystemcinema.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    Optional<Booking> findByBookingCode(String bookingCode);
    List<Booking> findByStatusAndBookingTimeBefore(BookingStatus status, LocalDateTime cutoff);
}
