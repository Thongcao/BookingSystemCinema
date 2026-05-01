package hsf302.bookingsystemcinema.service;

import hsf302.bookingsystemcinema.dto.RoomRequest;
import hsf302.bookingsystemcinema.dto.RoomResponse;
import hsf302.bookingsystemcinema.dto.SeatResponse;
import java.util.List;

public interface RoomService {
    List<RoomResponse> getAllRooms();
    List<RoomResponse> getRoomsByCinema(Long cinemaId);
    RoomResponse getRoomById(Long id);
    RoomResponse createRoom(RoomRequest request);
    void deleteRoom(Long id);
    List<SeatResponse> getSeatsByRoom(Long roomId);
}
