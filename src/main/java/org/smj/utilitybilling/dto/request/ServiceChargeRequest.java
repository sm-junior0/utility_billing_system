package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.smj.utilitybilling.enums.MeterType;
/**
 * Validated request DTO for ServiceChargeRequest API input.
 */

@Getter
@Setter
public class ServiceChargeRequest {

    @NotNull(message = "Meter type is required")
    @Schema(example = "WATER", description = "Allowed values: WATER, ELECTRICITY.")
    private MeterType meterType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
    @Schema(example = "500", description = "Fixed service charge amount in FRW.")
    private BigDecimal amount;

    @Schema(example = "true", description = "Whether this service charge is active for billing.")
    private Boolean active;
}
