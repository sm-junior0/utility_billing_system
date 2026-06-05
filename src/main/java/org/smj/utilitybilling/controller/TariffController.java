package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.TariffRequest;
import org.smj.utilitybilling.dto.response.TariffResponse;
import org.smj.utilitybilling.service.TariffService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * REST controller exposing Tariff API endpoints.
 */

@RestController
@RequestMapping("/api/tariffs")
@RequiredArgsConstructor
@Tag(name = "05 - Billing Configuration", description = "ROLE_ADMIN tariff and tier setup for future billing cycles")
@SecurityRequirement(name = "bearerAuth")
public class TariffController {

    private final TariffService tariffService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "Step 8: Admin creates a tariff version",
            description = "Protected endpoint. ROLE_ADMIN creates a flat or tier-based tariff for WATER or ELECTRICITY. Tariffs are versioned and new tariffs apply only to future billing cycles based on effective dates.")
    public ResponseEntity<TariffResponse> createTariff(@Valid @RequestBody TariffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tariffService.createTariff(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "List tariff versions",
            description = "Protected endpoint. ROLE_ADMIN and ROLE_FINANCE can inspect tariff versions, meter type, tariff type, effective dates, flat price, and tiers used by billing.")
    public List<TariffResponse> getAllTariffs() {
        return tariffService.getAllTariffs();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "Admin deactivates a tariff version",
            description = "Protected endpoint. ROLE_ADMIN marks a tariff inactive so it is no longer used for future generated bills.")
    public TariffResponse deactivateTariff(@Parameter(description = "Tariff ID to deactivate", required = true, example = "1") @PathVariable Long id) {
        return tariffService.deactivateTariff(id);
    }
}
