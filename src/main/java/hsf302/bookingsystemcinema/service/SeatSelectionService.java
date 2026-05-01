package hsf302.bookingsystemcinema.service;

import hsf302.bookingsystemcinema.dto.HoldSeatsResponse;
import hsf302.bookingsystemcinema.dto.SeatMapResponse;

import java.util.List;

public interface SeatSelectionService {

    SeatMapResponse getSeatMap(Long showtimeId, Long currentUserId);

    HoldSeatsResponse holdSeats(Long showtimeId, Long userId, List<Long> seatIds);

    void releaseSeats(Long showtimeId, Long userId, List<Long> seatIds);
}
