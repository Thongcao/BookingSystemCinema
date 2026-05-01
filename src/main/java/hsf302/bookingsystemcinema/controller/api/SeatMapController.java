package hsf302.bookingsystemcinema.controller.api;

import hsf302.bookingsystemcinema.dto.ApiResponse;
import hsf302.bookingsystemcinema.dto.HoldSeatsRequest;
import hsf302.bookingsystemcinema.dto.HoldSeatsResponse;
import hsf302.bookingsystemcinema.dto.SeatMapResponse;
import hsf302.bookingsystemcinema.service.SeatSelectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/showtimes")
@RequiredArgsConstructor
public class SeatMapController {

    private final SeatSelectionService seatSelectionService;

    /**
     * GET /api/public/showtimes/{id}/seat-map?userId=123
     * Returns merged seat map: DB status + Redis soft-lock status + ticket status.
     * userId is optional — used to mark which seats the current user is holding.
     */
    @GetMapping("/{id}/seat-map")
    public ResponseEntity<ApiResponse<SeatMapResponse>> getSeatMap(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        SeatMapResponse seatMap = seatSelectionService.getSeatMap(id, userId);
        return ResponseEntity.ok(ApiResponse.success(seatMap));
    }

    /**
     * POST /api/public/showtimes/{id}/hold-seats
     * Soft-locks selected seats in Redis with 5-minute TTL.
     * If ANY seat in the batch is already held by another user,
     * ALL seats in this request are rolled back automatically.
     */
    @PostMapping("/{id}/hold-seats")
    public ResponseEntity<ApiResponse<HoldSeatsResponse>> holdSeats(
            @PathVariable Long id,
            @Valid @RequestBody HoldSeatsRequest request) {
        HoldSeatsResponse response = seatSelectionService.holdSeats(id, request.getUserId(), request.getSeatIds());
        return ResponseEntity.ok(ApiResponse.success("Seats held successfully for 5 minutes", response));
    }

    /**
     * POST /api/public/showtimes/{id}/release-seats
     * Manually releases soft-locked seats (e.g. user deselects seats or navigates away).
     */
    @PostMapping("/{id}/release-seats")
    public ResponseEntity<ApiResponse<Void>> releaseSeats(
            @PathVariable Long id,
            @Valid @RequestBody HoldSeatsRequest request) {
        seatSelectionService.releaseSeats(id, request.getUserId(), request.getSeatIds());
        return ResponseEntity.ok(ApiResponse.success("Seats released successfully", null));
    }
}
