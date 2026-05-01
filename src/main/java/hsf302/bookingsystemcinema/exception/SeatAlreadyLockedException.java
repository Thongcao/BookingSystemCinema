package hsf302.bookingsystemcinema.exception;

public class SeatAlreadyLockedException extends RuntimeException {

    private final Long seatId;

    public SeatAlreadyLockedException(Long seatId, String heldByUserId) {
        super(String.format("Seat %d is already being held by another user", seatId));
        this.seatId = seatId;
    }

    public SeatAlreadyLockedException(Long seatId) {
        super(String.format("Seat %d is already booked or being held", seatId));
        this.seatId = seatId;
    }

    public Long getSeatId() {
        return seatId;
    }
}
