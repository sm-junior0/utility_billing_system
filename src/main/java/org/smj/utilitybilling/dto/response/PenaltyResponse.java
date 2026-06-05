package org.smj.utilitybilling.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * Response DTO returned by Penalty APIs.
 */

public record PenaltyResponse(
        Long id,
        String name,
        BigDecimal percentage,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
