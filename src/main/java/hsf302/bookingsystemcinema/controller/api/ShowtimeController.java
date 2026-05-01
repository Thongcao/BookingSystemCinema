package hsf302.bookingsystemcinema.controller.api;

import hsf302.bookingsystemcinema.dto.ApiResponse;
import hsf302.bookingsystemcinema.dto.ShowtimeRequest;
import hsf302.bookingsystemcinema.dto.ShowtimeResponse;
import hsf302.bookingsystemcinema.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(showtimeService.getAllShowtimes()));
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> getByMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.success(showtimeService.getShowtimesByMovie(movieId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(showtimeService.getShowtimeById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShowtimeResponse>> create(@Valid @RequestBody ShowtimeRequest request) {
        ShowtimeResponse created = showtimeService.createShowtime(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Showtime created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> update(@PathVariable Long id,
                                                                 @Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Showtime updated successfully",
                showtimeService.updateShowtime(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        showtimeService.deleteShowtime(id);
        return ResponseEntity.ok(ApiResponse.success("Showtime deleted successfully", null));
    }
}
