package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.ActiveStatusRequest;
import org.smj.utilitybilling.dto.request.TaxRequest;
import org.smj.utilitybilling.dto.response.TaxResponse;
import org.smj.utilitybilling.service.TaxService;
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
 * REST controller exposing Tax API endpoints.
 */

@RestController
@RequestMapping("/api/taxes")
@RequiredArgsConstructor
@Tag(name = "05 - Billing Configuration", description = "ROLE_ADMIN tax configuration such as VAT")
@SecurityRequirement(name = "bearerAuth")
public class TaxController {

    private final TaxService taxService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "Step 9: Admin creates a tax configuration",
            description = "Protected endpoint. ROLE_ADMIN configures VAT or another tax percentage independently from tariffs and service charges.")
    public ResponseEntity<TaxResponse> createTax(@Valid @RequestBody TaxRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxService.createTax(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "List tax configurations",
            description = "Protected endpoint. ROLE_ADMIN lists all configured tax records and active/inactive status.")
    public List<TaxResponse> getAllTaxes() {
        return taxService.getAllTaxes();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "Admin activates or deactivates a tax",
            description = "Protected endpoint. ROLE_ADMIN controls whether a tax configuration is currently used during bill calculation.")
    public TaxResponse updateStatus(@Parameter(description = "Tax ID whose active flag should be changed", required = true, example = "1") @PathVariable Long id, @Valid @RequestBody ActiveStatusRequest request) {
        return taxService.updateStatus(id, request.getActive());
    }
}
