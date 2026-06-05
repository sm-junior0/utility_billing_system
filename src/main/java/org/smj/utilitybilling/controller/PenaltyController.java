package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.ActiveStatusRequest;
import org.smj.utilitybilling.dto.request.PenaltyRequest;
import org.smj.utilitybilling.dto.response.PenaltyResponse;
import org.smj.utilitybilling.service.PenaltyService;
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
 * REST controller exposing Penalty API endpoints.
 */

@RestController
@RequestMapping("/api/penalties")
@RequiredArgsConstructor
@Tag(name = "05 - Billing Configuration", description = "ROLE_ADMIN late payment penalty configuration")
@SecurityRequirement(name = "bearerAuth")
public class PenaltyController {

    private final PenaltyService penaltyService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "Step 10: Admin creates a late payment penalty",
            description = "Protected endpoint. ROLE_ADMIN configures the percentage penalty applied when a bill remains unpaid after its due date.")
    public ResponseEntity<PenaltyResponse> createPenalty(@Valid @RequestBody PenaltyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(penaltyService.createPenalty(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "List penalty configurations",
            description = "Protected endpoint. ROLE_ADMIN lists configured late payment penalties and their active/inactive status.")
    public List<PenaltyResponse> getAllPenalties() {
        return penaltyService.getAllPenalties();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "05 - Billing Configuration",
            summary = "Admin activates or deactivates a penalty",
            description = "Protected endpoint. ROLE_ADMIN controls whether a penalty configuration is currently used for late bills.")
    public PenaltyResponse updateStatus(@Parameter(description = "Penalty ID whose active flag should be changed", required = true, example = "1") @PathVariable Long id, @Valid @RequestBody ActiveStatusRequest request) {
        return penaltyService.updateStatus(id, request.getActive());
    }
}
