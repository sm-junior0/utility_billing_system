package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.smj.utilitybilling.enums.MeterStatus;
/**
 * Validated request DTO for MeterStatusUpdateRequest API input.
 */

@Getter
@Setter
public class MeterStatusUpdateRequest {

    @NotNull(message = "Meter status is required")
    @Schema(example = "ACTIVE", description = "Allowed values: ACTIVE, INACTIVE.")
    private MeterStatus status;
}
