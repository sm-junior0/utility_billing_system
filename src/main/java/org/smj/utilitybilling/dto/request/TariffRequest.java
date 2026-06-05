package org.smj.utilitybilling.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.smj.utilitybilling.enums.MeterType;
import org.smj.utilitybilling.enums.TariffType;

/**
 * Validated tariff configuration input supporting flat or tiered pricing.
 */

@Getter
@Setter
public class TariffRequest {

    @NotNull(message = "Meter type is required")
    @Schema(example = "WATER", description = "Allowed values: WATER, ELECTRICITY.")
    private MeterType meterType;

    @NotNull(message = "Tariff type is required")
    @Schema(example = "FLAT", description = "Allowed values: FLAT, TIERED.")
    private TariffType tariffType;

    @NotNull(message = "Version is required")
    @Min(value = 1, message = "Tariff version must be at least 1")
    @Schema(example = "1", description = "Tariff version number. New versions apply to future billing cycles.")
    private Integer version;

    @NotNull(message = "Effective from date is required")
    @Schema(example = "2026-01-01", description = "Date from which this tariff can be used for bill generation.")
    private LocalDate effectiveFrom;

    @Schema(example = "2026-12-31", description = "Optional end date. Leave null for an open-ended active tariff.")
    private LocalDate effectiveTo;

    @DecimalMin(value = "0.0", inclusive = false, message = "Flat unit price must be greater than zero")
    @Schema(example = "340", description = "Unit price for FLAT tariffs. Leave null for TIERED tariffs.")
    private BigDecimal flatUnitPrice;

    @Valid
    @Schema(description = "Tier rows used only when tariffType is TIERED.")
    private List<TariffTierRequest> tiers;
}
