package org.smj.utilitybilling.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * Response DTO returned by Tax APIs.
 */

public record TaxResponse(
        Long id,
        String name,
        BigDecimal percentage,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
