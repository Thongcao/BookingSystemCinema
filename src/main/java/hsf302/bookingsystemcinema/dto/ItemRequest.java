package hsf302.bookingsystemcinema.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequest {

    @NotBlank(message = "Item name is required")
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0", message = "Price must be non-negative")
    private BigDecimal price;

    @NotBlank(message = "Item type is required (FOOD, BEVERAGE, COMBO)")
    private String type;
}
