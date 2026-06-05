package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * Validated tax configuration input such as VAT percentage.
 */

@Getter
@Setter
public class TaxRequest {

    @NotBlank(message = "Tax name is required")
    @Size(max = 100, message = "Tax name must not exceed 100 characters")
    @Schema(example = "VAT", description = "Tax name.")
    private String name;

    @NotNull(message = "Tax percentage is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Tax percentage must be greater than zero")
    @DecimalMax(value = "100.0", message = "Tax percentage cannot exceed 100")
    @Schema(example = "18", description = "Tax percentage applied during bill calculation.")
    private BigDecimal percentage;

    @Schema(example = "true", description = "Whether this tax is active for billing.")
    private Boolean active;
}
