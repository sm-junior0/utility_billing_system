package org.smj.utilitybilling.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.smj.utilitybilling.enums.PaymentMethod;
/**
 * Response DTO returned by Payment APIs.
 */

public record PaymentResponse(
        Long id,
        Long billId,
        String billReference,
        BigDecimal amountPaid,
        PaymentMethod paymentMethod,
        LocalDate paymentDate,
        String recordedBy,
        String approvedBy,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
