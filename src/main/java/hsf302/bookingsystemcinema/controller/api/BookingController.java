package hsf302.bookingsystemcinema.controller.api;

import hsf302.bookingsystemcinema.dto.ApiResponse;
import hsf302.bookingsystemcinema.dto.BookingResponse;
import hsf302.bookingsystemcinema.dto.CreateBookingRequest;
import hsf302.bookingsystemcinema.security.CustomUserPrincipal;
import hsf302.bookingsystemcinema.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * POST /api/public/bookings
     * Full checkout flow. UserId is extracted from the JWT token (SecurityContext),
     * NOT from the request body — preventing user impersonation.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        Long authenticatedUserId = principal.getUserId();
        BookingResponse response = bookingService.createBooking(request, authenticatedUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
    }

    /**
     * GET /api/public/bookings/{id}
     * Retrieve booking details with full breakdown.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingById(id)));
    }
}
