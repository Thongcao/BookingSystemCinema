package hsf302.bookingsystemcinema.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long bookingId;
    private String bookingCode;
    private String status;

    private String movieTitle;
    private String cinemaName;
    private String roomName;
    private LocalDateTime showStartTime;

    private List<TicketInfo> tickets;
    private List<ItemInfo> items;

    private BigDecimal ticketSubtotal;
    private BigDecimal itemSubtotal;
    private String voucherCode;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    private String paymentMethod;
    private String paymentStatus;
    private String paymentTransactionId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketInfo {
        private Long ticketId;
        private String seatRow;
        private Integer seatNumber;
        private String seatType;
        private String ticketType;
        private BigDecimal price;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemInfo {
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}
