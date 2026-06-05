package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.smj.utilitybilling.enums.PaymentMethod;

/**
 * Validated legacy electricity prepaid top-up input.
 */

@Getter
@Setter
public class PrepaidTopUpRequest {

    @NotNull(message = "Meter ID is required")
    @Positive(message = "Meter ID must be a positive number")
    @Schema(example = "2", description = "Electricity meter ID for the legacy prepaid top-up.")
    private Long meterId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
    @Schema(example = "2000", description = "Top-up amount in FRW.")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    @Schema(example = "MOBILE_MONEY", description = "Allowed values: CASH, MOBILE_MONEY, BANK_TRANSFER, CARD.")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    @Schema(example = "2026-06-05", description = "Date when the prepaid transaction was recorded.")
    private LocalDate transactionDate;
}
