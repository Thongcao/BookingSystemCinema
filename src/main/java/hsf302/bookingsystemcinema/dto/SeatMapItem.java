package hsf302.bookingsystemcinema.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapItem {
    private Long seatId;
    private String seatRow;
    private Integer seatNumber;
    private Integer gridRow;
    private Integer gridColumn;
    private String seatType;
    private String displayStatus;
    private BigDecimal surcharge;
    private String heldByCurrentUser;
}
