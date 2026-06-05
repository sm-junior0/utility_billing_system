package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
/**
 * Validated request DTO for ResendOtpRequest API input.
 */

@Getter
@Setter
@Schema(description = "Request to resend a registration OTP for a user who has not yet verified the account.")
public class ResendOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "alice@example.com")
    private String email;
}
