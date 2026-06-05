package org.smj.utilitybilling.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.smj.utilitybilling.enums.PaymentMethod;

/**
 * Validated payment input. Requires either billId or billReference plus amount, method, and date.
 */

@Getter
@Setter
@Schema(
        description = "Record a partial or full payment against an approved bill. Use billId OR billReference, not both.",
        example = """
                {
                  "billId": 3,
                  "amountPaid": 15000,
                  "paymentMethod": "MOBILE_MONEY",
                  "paymentDate": "2026-06-05"
                }
                """)
public class PaymentRequest {

    @Positive(message = "Bill ID must be a positive number")
    @Schema(description = "Approved bill ID receiving the payment. Omit when using billReference.")
    private Long billId;

    @Size(max = 50, message = "Bill reference must not exceed 50 characters")
    @Schema(description = "Approved bill reference such as BILL-202608-XXXXXXXX. Omit when using billId.")
    private String billReference;

    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount paid must be greater than zero")
    @Schema(example = "15000", description = "Payment amount. Use the exact outstandingBalance for a full payment.")
    private BigDecimal amountPaid;

    @NotNull(message = "Payment method is required")
    @Schema(example = "MOBILE_MONEY", description = "Allowed values: CASH, MOBILE_MONEY, BANK_TRANSFER, CARD.")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    @Schema(example = "2026-06-05", description = "Date when the payment was received.")
    private LocalDate paymentDate;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Either billId or billReference is required")
    public boolean isBillTargetProvided() {
        return billId != null || (billReference != null && !billReference.isBlank());
    }
}
