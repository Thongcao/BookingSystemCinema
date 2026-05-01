package hsf302.bookingsystemcinema.service;

import hsf302.bookingsystemcinema.dto.BookingResponse;
import hsf302.bookingsystemcinema.dto.CreateBookingRequest;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request, Long authenticatedUserId);

    BookingResponse getBookingById(Long bookingId);
}
