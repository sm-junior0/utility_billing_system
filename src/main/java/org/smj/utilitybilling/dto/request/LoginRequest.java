package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Validated login credentials (email and password).
 */

@Getter
@Setter
@Schema(
        description = "Login with email and password to receive a JWT.",
        example = """
                {
                  "email": "juniormanene@gmail.com",
                  "password": "Test@1234"
                }
                """)
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "juniormanene@gmail.com", description = "User email/username.")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Schema(example = "Test@1234", description = "Plain password submitted during login. Stored passwords are BCrypt encrypted.")
    private String password;
}
