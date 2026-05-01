package hsf302.bookingsystemcinema.service.impl;

import hsf302.bookingsystemcinema.dto.RoomRequest;
import hsf302.bookingsystemcinema.dto.RoomResponse;
import hsf302.bookingsystemcinema.dto.SeatResponse;
import hsf302.bookingsystemcinema.entity.Cinema;
import hsf302.bookingsystemcinema.entity.Room;
import hsf302.bookingsystemcinema.entity.Seat;
import hsf302.bookingsystemcinema.entity.enums.SeatStatus;
import hsf302.bookingsystemcinema.entity.enums.SeatType;
import hsf302.bookingsystemcinema.exception.ResourceNotFoundException;
import hsf302.bookingsystemcinema.repository.CinemaRepository;
import hsf302.bookingsystemcinema.repository.RoomRepository;
import hsf302.bookingsystemcinema.repository.SeatRepository;
import hsf302.bookingsystemcinema.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final CinemaRepository cinemaRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByCinema(Long cinemaId) {
        return roomRepository.findByCinemaId(cinemaId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        return toResponse(findRoomOrThrow(id));
    }

    @Override
    public RoomResponse createRoom(RoomRequest request) {
        Cinema cinema = cinemaRepository.findById(request.getCinemaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cinema", "id", request.getCinemaId()));

        int totalSeats = request.getRows() * request.getSeatsPerRow();

        Room room = Room.builder()
                .cinema(cinema)
                .name(request.getName())
                .totalSeats(totalSeats)
                .build();
        room = roomRepository.save(room);

        generateSeats(room, request.getRows(), request.getSeatsPerRow(), request.getVipFromRow());

        return toResponse(room);
    }

    @Override
    public void deleteRoom(Long id) {
        Room room = findRoomOrThrow(id);
        roomRepository.delete(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByRoom(Long roomId) {
        findRoomOrThrow(roomId);
        return seatRepository.findByRoomIdOrderByGridRowAscGridColumnAsc(roomId).stream()
                .map(this::toSeatResponse)
                .collect(Collectors.toList());
    }

    private void generateSeats(Room room, int rows, int seatsPerRow, Integer vipFromRow) {
        List<Seat> seats = new ArrayList<>();
        int vipStart = (vipFromRow != null) ? vipFromRow : rows;

        for (int r = 0; r < rows; r++) {
            String rowLabel = String.valueOf((char) ('A' + r));
            SeatType type = (r >= vipStart) ? SeatType.VIP : SeatType.NORMAL;

            for (int c = 1; c <= seatsPerRow; c++) {
                seats.add(Seat.builder()
                        .room(room)
                        .seatRow(rowLabel)
                        .seatNumber(c)
                        .gridRow(r)
                        .gridColumn(c - 1)
                        .type(type)
                        .status(SeatStatus.AVAILABLE)
                        .build());
            }
        }
        seatRepository.saveAll(seats);
    }

    private Room findRoomOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", id));
    }

    private RoomResponse toResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .cinemaId(room.getCinema().getId())
                .cinemaName(room.getCinema().getName())
                .name(room.getName())
                .totalSeats(room.getTotalSeats())
                .build();
    }

    private SeatResponse toSeatResponse(Seat seat) {
        return SeatResponse.builder()
                .id(seat.getId())
                .seatRow(seat.getSeatRow())
                .seatNumber(seat.getSeatNumber())
                .gridRow(seat.getGridRow())
                .gridColumn(seat.getGridColumn())
                .type(seat.getType().name())
                .status(seat.getStatus().name())
                .build();
    }
}
