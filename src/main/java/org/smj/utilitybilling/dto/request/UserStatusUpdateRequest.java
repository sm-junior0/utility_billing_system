package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.smj.utilitybilling.enums.UserStatus;
/**
 * Validated request DTO for UserStatusUpdateRequest API input.
 */

@Getter
@Setter
public class UserStatusUpdateRequest {

    @NotNull(message = "Status is required")
    @Schema(example = "ACTIVE", description = "Allowed values: ACTIVE, PENDING_VERIFICATION, INACTIVE.")
    private UserStatus status;
}
