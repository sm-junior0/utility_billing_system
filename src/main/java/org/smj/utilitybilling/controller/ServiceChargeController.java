package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.ActiveStatusRequest;
import org.smj.utilitybilling.dto.request.ServiceChargeRequest;
import org.smj.utilitybilling.dto.response.ServiceChargeResponse;
import org.smj.utilitybilling.service.ServiceChargeService;
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
 * REST controller exposing ServiceCharge API endpoints.
 */

@RestController
@RequestMapping("/api/service-charges")
@RequiredArgsConstructor
@Tag(name = "05 - Billing Configuration", description = "ROLE_ADMIN fixed monthly service charge configuration")
@SecurityRequirement(name = "bearerAuth")
public class ServiceChargeController {

    private final ServiceChargeService serviceChargeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "Admin creates a fixed service charge",
            description = "Protected endpoint. ROLE_ADMIN configures a fixed monthly service charge for WATER or ELECTRICITY independently from consumption tariffs.")
    public ResponseEntity<ServiceChargeResponse> createServiceCharge(@Valid @RequestBody ServiceChargeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceChargeService.createServiceCharge(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "List service charges",
            description = "Protected endpoint. ROLE_ADMIN lists fixed monthly service charges by meter type and active/inactive status.")
    public List<ServiceChargeResponse> getAllServiceCharges() {
        return serviceChargeService.getAllServiceCharges();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "Admin activates or deactivates a service charge",
            description = "Protected endpoint. ROLE_ADMIN controls whether a fixed service charge is currently used during bill calculation.")
    public ServiceChargeResponse updateStatus(@Parameter(description = "Service charge ID whose active flag should be changed", required = true, example = "1") @PathVariable Long id, @Valid @RequestBody ActiveStatusRequest request) {
        return serviceChargeService.updateStatus(id, request.getActive());
    }
}
