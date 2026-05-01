package hsf302.bookingsystemcinema.controller.api;

import hsf302.bookingsystemcinema.dto.ApiResponse;
import hsf302.bookingsystemcinema.dto.RoomRequest;
import hsf302.bookingsystemcinema.dto.RoomResponse;
import hsf302.bookingsystemcinema.dto.SeatResponse;
import hsf302.bookingsystemcinema.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(roomService.getAllRooms()));
    }

    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getByCinema(@PathVariable Long cinemaId) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoomsByCinema(cinemaId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getRoomById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> create(@Valid @RequestBody RoomRequest request) {
        RoomResponse created = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created with seats auto-generated", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roomService.getSeatsByRoom(id)));
    }
}
