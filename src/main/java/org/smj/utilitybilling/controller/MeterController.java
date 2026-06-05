package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.MeterRequest;
import org.smj.utilitybilling.dto.request.MeterStatusUpdateRequest;
import org.smj.utilitybilling.dto.response.MeterResponse;
import org.smj.utilitybilling.dto.response.PageResponse;
import org.smj.utilitybilling.service.MeterService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * REST controller exposing Meter API endpoints.
 */

@RestController
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@Tag(name = "04 - Meter Setup", description = "Meter setup, customer meter ownership, and meter lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class MeterController {

    private final MeterService meterService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "04 - Meter Setup",
            summary = "Step 7: Admin creates a customer meter",
            description = "Protected endpoint. ROLE_ADMIN registers a unique meter number, selects WATER or ELECTRICITY, links it to a customer, and sets active/inactive status.")
    public ResponseEntity<MeterResponse> createMeter(@Valid @RequestBody MeterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meterService.createMeter(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    @Operation(
            tags = "04 - Meter Setup",
            summary = "List meters for staff users (paginated)",
            description = "Protected endpoint. ROLE_ADMIN, ROLE_FINANCE, and ROLE_OPERATOR can list all meters for setup, reading, and billing workflows. Query params: page (0-based), size (default 10), sort (allowed: id, meterNumber, meterType, installationDate, status, createdAt — e.g. sort=meterNumber,asc).")
    public PageResponse<MeterResponse> getAllMeters(
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return meterService.getAllMeters(pageable);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR', 'CUSTOMER')")
    @Operation(
            tags = "09 - Customer Self-Service",
            summary = "View meters owned by a customer (paginated)",
            description = "Protected endpoint. Staff can view meters for any customer. ROLE_CUSTOMER can only view meters belonging to their linked customer profile. Supports page, size, and sort query params.")
    public PageResponse<MeterResponse> getMetersByCustomer(
            @Parameter(description = "Customer ID whose meters should be listed", required = true, example = "1") @PathVariable Long customerId,
            Authentication authentication,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return meterService.getMetersByCustomer(customerId, authentication.getName(), isCustomer(authentication), pageable);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "04 - Meter Setup",
            summary = "Admin changes meter status",
            description = "Protected endpoint. ROLE_ADMIN activates or deactivates a meter. Inactive meters cannot receive new meter readings.")
    public MeterResponse updateStatus(@Parameter(description = "Meter ID whose status should be changed", required = true, example = "1") @PathVariable Long id, @Valid @RequestBody MeterStatusUpdateRequest request) {
        return meterService.updateStatus(id, request.getStatus());
    }

    private boolean isCustomer(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER"));
    }
}
