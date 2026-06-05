package org.smj.utilitybilling.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
/**
 * Validated request DTO for TariffTierRequest API input.
 */

@Getter
@Setter
public class TariffTierRequest {

    @NotNull(message = "Minimum units are required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum units must be zero or greater")
    @Schema(example = "0", description = "Inclusive lower consumption boundary for this tier.")
    private BigDecimal minUnits;

    @DecimalMin(value = "0.0", inclusive = false, message = "Maximum units must be greater than zero")
    @Schema(example = "50", description = "Exclusive upper consumption boundary. Use null for the last unlimited tier.")
    private BigDecimal maxUnits;

    @NotNull(message = "Price per unit is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price per unit must be greater than zero")
    @Schema(example = "100", description = "Price per unit consumed inside this tier.")
    private BigDecimal pricePerUnit;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Maximum units must be greater than minimum units when provided")
    public boolean isValidTierRange() {
        if (minUnits == null || maxUnits == null) {
            return true;
        }
        return maxUnits.compareTo(minUnits) > 0;
    }
}
