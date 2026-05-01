package hsf302.bookingsystemcinema.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {

    @NotNull(message = "Cinema ID is required")
    private Long cinemaId;

    @NotBlank(message = "Room name is required")
    private String name;

    @NotNull(message = "Number of rows is required")
    @Min(value = 1, message = "Rows must be at least 1")
    private Integer rows;

    @NotNull(message = "Seats per row is required")
    @Min(value = 1, message = "Seats per row must be at least 1")
    private Integer seatsPerRow;

    private Integer vipFromRow;
}
