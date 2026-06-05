package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.PrepaidTopUpRequest;
import org.smj.utilitybilling.dto.response.PrepaidTransactionResponse;
import org.smj.utilitybilling.service.PrepaidTransactionService;
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
 * REST controller exposing PrepaidTransaction API endpoints.
 */

@RestController
@RequestMapping("/api/prepaid")
@RequiredArgsConstructor
@Tag(name = "11 - Legacy Prepaid Support", description = "Small legacy prepaid electricity support while the system transitions to postpaid")
@SecurityRequirement(name = "bearerAuth")
public class PrepaidTransactionController {

    private final PrepaidTransactionService prepaidTransactionService;

    @PostMapping("/topup")
    @PreAuthorize("hasRole('FINANCE')")
    @Operation(
            tags = "11 - Legacy Prepaid Support",
            summary = "Legacy electricity prepaid top-up",
            description = "Protected endpoint. ROLE_FINANCE records a simple legacy electricity prepaid top-up for an ELECTRICITY meter and receives a generated token. This is kept small because the main exam workflow transitions electricity to postpaid billing.")
    public ResponseEntity<PrepaidTransactionResponse> topUp(@Valid @RequestBody PrepaidTopUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prepaidTransactionService.topUp(request));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('FINANCE', 'CUSTOMER')")
    @Operation(
            tags = "09 - Customer Self-Service",
            summary = "View legacy prepaid history",
            description = "Protected endpoint. ROLE_FINANCE can view prepaid history for a customer. ROLE_CUSTOMER can only view prepaid history for their linked customer profile.")
    public List<PrepaidTransactionResponse> getTransactionsByCustomer(
            @Parameter(description = "Customer ID whose legacy prepaid transactions should be listed", required = true, example = "1") @PathVariable Long customerId,
            Authentication authentication) {
        return prepaidTransactionService.getTransactionsByCustomer(customerId, authentication.getName(), isCustomer(authentication));
    }

    private boolean isCustomer(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER"));
    }
}
