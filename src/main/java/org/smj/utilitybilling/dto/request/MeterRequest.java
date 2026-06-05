package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.smj.utilitybilling.enums.MeterStatus;
import org.smj.utilitybilling.enums.MeterType;

/**
 * Validated meter registration input linked to a customer.
 */

@Getter
@Setter
@Schema(
        description = "Register a water or electricity meter for an existing customer.",
        example = """
                {
                  "meterNumber": "WTR-0001",
                  "meterType": "WATER",
                  "installationDate": "2026-01-10",
                  "status": "ACTIVE",
                  "customerId": 1
                }
                """)
public class MeterRequest {

    @NotBlank(message = "Meter number is required")
    @Size(max = 50, message = "Meter number must not exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Meter number may contain letters, numbers, and hyphens only")
    @Schema(example = "WTR-0001", description = "Unique utility meter number.")
    private String meterNumber;

    @NotNull(message = "Meter type is required")
    @Schema(example = "WATER", description = "Allowed values: WATER, ELECTRICITY.")
    private MeterType meterType;

    @NotNull(message = "Installation date is required")
    @PastOrPresent(message = "Installation date cannot be in the future")
    @Schema(example = "2026-01-10", description = "Date the meter was installed.")
    private LocalDate installationDate;

    @Schema(example = "ACTIVE", description = "Meter status. Inactive meters cannot receive readings.")
    private MeterStatus status;

    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be a positive number")
    @Schema(example = "1", description = "Customer ID that owns this meter.")
    private Long customerId;
}
