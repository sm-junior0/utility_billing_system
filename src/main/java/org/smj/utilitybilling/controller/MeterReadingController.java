package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.MeterReadingRequest;
import org.smj.utilitybilling.dto.response.MeterReadingResponse;
import org.smj.utilitybilling.service.MeterReadingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * REST controller exposing MeterReading API endpoints.
 */

@RestController
@RequestMapping("/api/readings")
@RequiredArgsConstructor
@Tag(name = "06 - Meter Reading Operations", description = "ROLE_OPERATOR monthly utility reading capture and reading lookup")
@SecurityRequirement(name = "bearerAuth")
public class MeterReadingController {

    private final MeterReadingService meterReadingService;

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(
            tags = "06 - Meter Reading Operations",
            summary = "Step 11: Operator captures a monthly meter reading",
            description = "Protected endpoint. ROLE_OPERATOR records previous reading, current reading, and reading date for an active meter. Business rules: current reading must be greater than previous reading, the meter must be active, and only one reading is allowed per meter per month/year.")
    public ResponseEntity<MeterReadingResponse> createReading(
            @Valid @RequestBody MeterReadingRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meterReadingService.createReading(request, authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    @Operation(
            tags = "06 - Meter Reading Operations",
            summary = "List captured readings",
            description = "Protected endpoint. ROLE_ADMIN, ROLE_FINANCE, and ROLE_OPERATOR can review all captured readings before bill generation.")
    public List<MeterReadingResponse> getAllReadings() {
        return meterReadingService.getAllReadings();
    }

    @GetMapping("/meter/{meterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    @Operation(
            tags = "06 - Meter Reading Operations",
            summary = "Get readings for one meter",
            description = "Protected endpoint. ROLE_ADMIN, ROLE_FINANCE, and ROLE_OPERATOR can retrieve readings for a specific meter to confirm monthly consumption.")
    public List<MeterReadingResponse> getReadingsByMeter(@Parameter(description = "Meter ID whose readings should be listed", required = true, example = "1") @PathVariable Long meterId) {
        return meterReadingService.getReadingsByMeter(meterId);
    }
}
