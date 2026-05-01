package hsf302.bookingsystemcinema.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatBookingRequest {

    @NotNull(message = "Seat ID is required")
    private Long seatId;

    private String ticketType;
}
