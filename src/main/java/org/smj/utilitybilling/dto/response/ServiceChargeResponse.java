package org.smj.utilitybilling.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.smj.utilitybilling.enums.MeterType;
/**
 * Response DTO returned by ServiceCharge APIs.
 */

public record ServiceChargeResponse(
        Long id,
        MeterType meterType,
        BigDecimal amount,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
