package hsf302.bookingsystemcinema.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapResponse {
    private Long showtimeId;
    private String movieTitle;
    private String roomName;
    private String cinemaName;
    private BigDecimal basePrice;
    private Integer totalRows;
    private Integer totalColumns;
    private List<SeatMapItem> seats;
}
