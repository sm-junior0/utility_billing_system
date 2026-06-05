package org.smj.utilitybilling.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.smj.utilitybilling.enums.MeterType;
import org.smj.utilitybilling.enums.TariffType;
/**
 * Response DTO returned by Tariff APIs.
 */

public record TariffResponse(
        Long id,
        MeterType meterType,
        TariffType tariffType,
        Integer version,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        BigDecimal flatUnitPrice,
        List<TariffTierResponse> tiers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
