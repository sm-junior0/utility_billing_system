package org.smj.utilitybilling.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.smj.utilitybilling.enums.PaymentMethod;
/**
 * Response DTO returned by PrepaidTransaction APIs.
 */

public record PrepaidTransactionResponse(
        Long id,
        Long meterId,
        String meterNumber,
        Long customerId,
        String customerName,
        BigDecimal amount,
        String token,
        BigDecimal unitsPurchased,
        PaymentMethod paymentMethod,
        LocalDate transactionDate,
        LocalDateTime createdAt
) {
}
