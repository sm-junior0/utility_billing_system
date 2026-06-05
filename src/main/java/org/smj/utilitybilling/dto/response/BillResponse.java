package org.smj.utilitybilling.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.smj.utilitybilling.enums.BillStatus;
/**
 * Response DTO returned by Bill APIs.
 */

public record BillResponse(
        Long id,
        String billReference,
        Long customerId,
        String customerName,
        Long meterId,
        String meterNumber,
        Integer billingMonth,
        Integer billingYear,
        LocalDate dueDate,
        BigDecimal consumption,
        BigDecimal tariffAmount,
        BigDecimal serviceChargeAmount,
        BigDecimal taxAmount,
        BigDecimal penaltyAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal outstandingBalance,
        BillStatus status,
        String approvedBy,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
