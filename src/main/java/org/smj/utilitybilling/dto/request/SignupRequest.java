package org.smj.utilitybilling.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Validated public signup input. Role is always assigned as ROLE_CUSTOMER after OTP verification.
 */

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        description = "Public signup request. Every new user is automatically created as ROLE_CUSTOMER with PENDING_VERIFICATION status.",
        example = """
                {
                  "fullNames": "Alice Uwase",
                  "email": "jj3032205@gmail.com",
                  "phoneNumber": "250788123456",
                  "password": "Test@1234"
                }
                """)
public class SignupRequest {

    @NotBlank(message = "Full names are required")
    @Size(max = 120, message = "Full names must not exceed 120 characters")
    @Schema(example = "Alice Uwase")
    private String fullNames;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "jj3032205@gmail.com")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+]{10,15}$", message = "Phone number must contain 10 to 15 digits")
    @Schema(example = "250788123456")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Schema(example = "Test@1234")
    private String password;
}
