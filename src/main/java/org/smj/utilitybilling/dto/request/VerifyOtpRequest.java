package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
/**
 * Validated request DTO for VerifyOtpRequest API input.
 */

@Getter
@Setter
@Schema(
        description = "OTP verification request used to activate a newly registered account.",
        example = """
                {
                  "email": "jj3032205@gmail.com",
                  "otpCode": "123456"
                }
                """)
public class VerifyOtpRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "jj3032205@gmail.com")
    private String email;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP code must contain exactly 6 digits")
    @Schema(example = "123456")
    private String otpCode;
}
