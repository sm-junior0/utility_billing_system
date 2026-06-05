package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
/**
 * Validated request DTO for ActiveStatusRequest API input.
 */

@Getter
@Setter
public class ActiveStatusRequest {

    @NotNull(message = "Active status is required")
    @Schema(example = "true", description = "true activates the configuration; false deactivates it.")
    private Boolean active;
}
