package org.smj.utilitybilling.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * Validated meter reading capture input used by operators.
 */

@Getter
@Setter
@Schema(
        description = "Capture one monthly reading for an active meter. Use a new month/year for each new bill.",
        example = """
                {
                  "meterId": 1,
                  "previousReading": 135,
                  "currentReading": 168,
                  "readingDate": "2026-08-01"
                }
                """)
public class MeterReadingRequest {

    @NotNull(message = "Meter ID is required")
    @Positive(message = "Meter ID must be a positive number")
    @Schema(example = "1", description = "Active meter ID being read. Water meter WTR-0001 is usually id 1.")
    private Long meterId;

    @NotNull(message = "Previous reading is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Previous reading must be zero or greater")
    @Schema(example = "135", description = "Previous meter reading value from the last captured reading.")
    private BigDecimal previousReading;

    @NotNull(message = "Current reading is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Current reading must be greater than zero")
    @Schema(example = "168", description = "Current meter reading value. Must be greater than previousReading.")
    private BigDecimal currentReading;

    @NotNull(message = "Reading date is required")
    @PastOrPresent(message = "Reading date cannot be in the future")
    @Schema(example = "2026-08-01", description = "Date of the reading. Only one reading is allowed per meter per month/year.")
    private LocalDate readingDate;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Current reading must be greater than previous reading")
    public boolean isCurrentReadingGreaterThanPrevious() {
        if (previousReading == null || currentReading == null) {
            return true;
        }
        return currentReading.compareTo(previousReading) > 0;
    }
}
