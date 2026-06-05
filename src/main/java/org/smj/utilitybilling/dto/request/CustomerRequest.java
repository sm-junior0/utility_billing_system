package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.smj.utilitybilling.enums.UserStatus;

/**
 * Validated customer profile input including unique national ID, email, and phone checks.
 */

@Getter
@Setter
@Schema(
        description = "Create or update a customer profile and optionally link it to a signup user account.",
        example = """
                {
                  "fullNames": "Alice Uwase",
                  "nationalId": "1199912345678901",
                  "email": "jj3032205@gmail.com",
                  "phoneNumber": "250788123456",
                  "address": "Kigali, Gasabo",
                  "status": "ACTIVE",
                  "ownerUserId": 2
                }
                """)
public class CustomerRequest {

    @NotBlank(message = "Full names are required")
    @Size(max = 120, message = "Full names must not exceed 120 characters")
    @Schema(example = "Alice Uwase", description = "Customer full legal names.")
    private String fullNames;

    @NotBlank(message = "National ID is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "National ID must contain exactly 16 digits")
    @Schema(example = "1199912345678901", description = "Unique 16-digit Rwandan National ID.")
    private String nationalId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "jj3032205@gmail.com", description = "Customer contact email. Use the same inbox for bill/payment notification emails.")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+]{10,15}$", message = "Phone number must contain 10 to 15 digits")
    @Schema(example = "250788123456", description = "Customer phone number.")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Schema(example = "Kigali, Gasabo", description = "Customer service address.")
    private String address;

    @Schema(example = "ACTIVE", description = "Customer billing status. Inactive customers cannot receive bills.")
    private UserStatus status;

    @Positive(message = "Owner user ID must be a positive number")
    @Schema(example = "2", description = "UserAccount ID from signup response userId. Alice is usually id 2 after admin id 1.")
    private Long ownerUserId;
}
