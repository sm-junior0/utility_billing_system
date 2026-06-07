package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Validated request DTO for ForgotPasswordRequest API input.
 */

@Getter
@Setter
@Schema(
        description = "Request a one-time password for resetting an existing account password.",
        example = """
                {
                  "email": "jj3032205@gmail.com"
                }
                """)
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "jj3032205@gmail.com", description = "Existing account email that should receive the password reset OTP.")
    private String email;
}
