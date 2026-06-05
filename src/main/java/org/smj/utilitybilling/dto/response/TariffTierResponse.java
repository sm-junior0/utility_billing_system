package org.smj.utilitybilling.dto.response;

import java.math.BigDecimal;
/**
 * Response DTO returned by TariffTier APIs.
 */

public record TariffTierResponse(
        Long id,
        BigDecimal minUnits,
        BigDecimal maxUnits,
        BigDecimal pricePerUnit
) {
}
