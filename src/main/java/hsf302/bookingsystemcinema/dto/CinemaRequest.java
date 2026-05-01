package hsf302.bookingsystemcinema.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaRequest {

    @NotBlank(message = "Cinema name is required")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;
}
