package hsf302.bookingsystemcinema.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRequest {

    @NotBlank(message = "Voucher code is required")
    @Size(max = 30, message = "Voucher code must not exceed 30 characters")
    private String code;

    @NotNull(message = "Discount percent is required")
    @DecimalMin(value = "0.01", message = "Discount percent must be greater than 0")
    @DecimalMax(value = "100.00", message = "Discount percent must not exceed 100")
    private BigDecimal discountPercent;

    @NotNull(message = "Max discount amount is required")
    @DecimalMin(value = "0", message = "Max discount must be non-negative")
    private BigDecimal maxDiscount;

    @NotNull(message = "Valid until date is required")
    private LocalDateTime validUntil;

    @NotNull(message = "Usage limit is required")
    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;
}
