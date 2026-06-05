package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Validated late-payment penalty configuration input.
 */

@Getter
@Setter
public class PenaltyRequest {

    @NotBlank(message = "Penalty name is required")
    @Size(max = 100, message = "Penalty name must not exceed 100 characters")
    @Schema(example = "Late Payment Penalty", description = "Penalty configuration name.")
    private String name;

    @NotNull(message = "Penalty percentage is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Penalty percentage must be greater than zero")
    @DecimalMax(value = "100.0", message = "Penalty percentage cannot exceed 100")
    @Schema(example = "5", description = "Penalty percentage applied when a bill is paid after its due date.")
    private BigDecimal percentage;

    @Schema(example = "true", description = "Whether this penalty is active for billing.")
    private Boolean active;
}
