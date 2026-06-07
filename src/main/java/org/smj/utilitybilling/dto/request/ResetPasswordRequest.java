package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Validated request DTO for ResetPasswordRequest API input.
 */

@Getter
@Setter
@Schema(
        description = "Reset a password using the OTP sent by the forgot-password endpoint.",
        example = """
                {
                  "email": "jj3032205@gmail.com",
                  "otpCode": "123456",
                  "newPassword": "NewPass@123"
                }
                """)
public class ResetPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "jj3032205@gmail.com", description = "Existing account email being reset.")
    private String email;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP code must contain exactly 6 digits")
    @Schema(example = "123456", description = "Password reset OTP sent to the email address.")
    private String otpCode;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters")
    @Schema(example = "NewPass@123", description = "Replacement plain password that will be BCrypt-encoded before storage.")
    private String newPassword;
}
