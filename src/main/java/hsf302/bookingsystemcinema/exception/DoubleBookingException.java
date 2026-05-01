package hsf302.bookingsystemcinema.exception;

public class DoubleBookingException extends RuntimeException {

    public DoubleBookingException(Long showtimeId, Long seatId) {
        super(String.format("Seat %d has already been booked for showtime %d. Transaction rolled back.", seatId, showtimeId));
    }

    public DoubleBookingException(String message) {
        super(message);
    }
}
