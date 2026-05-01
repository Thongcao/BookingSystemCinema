package hsf302.bookingsystemcinema.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private String seatRow;
    private Integer seatNumber;
    private Integer gridRow;
    private Integer gridColumn;
    private String type;
    private String status;
}
