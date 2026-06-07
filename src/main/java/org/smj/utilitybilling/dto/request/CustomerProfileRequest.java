package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Customer self-service profile submission captured after signup and OTP verification.
 */

@Getter
@Setter
@Schema(
        description = "Submit the billing customer profile linked to the logged-in customer account.",
        example = """
                {
                  "nationalId": "1199912345678901",
                  "address": "Kigali, Gasabo"
                }
                """)
public class CustomerProfileRequest {

    @NotBlank(message = "National ID is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "National ID must contain exactly 16 digits")
    @Schema(example = "1199912345678901", description = "Unique 16-digit Rwandan National ID.")
    private String nationalId;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Schema(example = "Kigali, Gasabo", description = "Customer service address.")
    private String address;
}
