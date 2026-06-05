package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.smj.utilitybilling.enums.Role;
/**
 * Validated request DTO for UserRoleUpdateRequest API input.
 */

@Getter
@Setter
public class UserRoleUpdateRequest {

    @NotNull(message = "Role is required")
    @Schema(example = "ROLE_OPERATOR", description = "Only ROLE_ADMIN can assign or change user roles.")
    private Role role;
}
