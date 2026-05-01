package hsf302.bookingsystemcinema.controller.api;

import hsf302.bookingsystemcinema.dto.ApiResponse;
import hsf302.bookingsystemcinema.dto.CinemaRequest;
import hsf302.bookingsystemcinema.dto.CinemaResponse;
import hsf302.bookingsystemcinema.service.CinemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cinemas")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CinemaResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(cinemaService.getAllCinemas()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(cinemaService.getCinemaById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CinemaResponse>> create(@Valid @RequestBody CinemaRequest request) {
        CinemaResponse created = cinemaService.createCinema(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cinema created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CinemaResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody CinemaRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cinema updated successfully",
                cinemaService.updateCinema(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        cinemaService.deleteCinema(id);
        return ResponseEntity.ok(ApiResponse.success("Cinema deleted successfully", null));
    }
}
