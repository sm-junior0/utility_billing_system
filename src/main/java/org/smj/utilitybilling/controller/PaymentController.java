package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.PaymentRequest;
import org.smj.utilitybilling.dto.response.PaymentResponse;
import org.smj.utilitybilling.service.PaymentService;
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
 * REST controller exposing Payment API endpoints.
 */

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "08 - Payment Operations", description = "Payment recording, balance updates, and payment history")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(
            tags = "08 - Payment Operations",
            summary = "Step 14: Finance records a customer payment",
            description = "Protected endpoint. ROLE_FINANCE records partial or full payment. The bill balance updates immediately, PARTIALLY_PAID is used for partial payments, PAID is used when outstanding balance reaches zero, and the full-payment database trigger creates a customer notification.")
    public ResponseEntity<PaymentResponse> recordPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.recordPayment(request, authentication.getName()));
    }

    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(
            tags = "09 - Customer Self-Service",
            summary = "View payment history for one bill",
            description = "Protected endpoint. Staff can view payment history for any bill. ROLE_CUSTOMER can only view payment records for bills belonging to their linked customer profile.")
    public List<PaymentResponse> getPaymentsByBill(@Parameter(description = "Bill ID whose payments should be listed", required = true, example = "1") @PathVariable Long billId, Authentication authentication) {
        return paymentService.getPaymentsByBill(billId, authentication.getName(), isCustomer(authentication));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(
            tags = "09 - Customer Self-Service",
            summary = "View payment history for a customer",
            description = "Protected endpoint. Staff can view payment history for any customer. ROLE_CUSTOMER can only view their own linked customer payment history.")
    public List<PaymentResponse> getPaymentsByCustomer(@Parameter(description = "Customer ID whose payments should be listed", required = true, example = "1") @PathVariable Long customerId, Authentication authentication) {
        return paymentService.getPaymentsByCustomer(customerId, authentication.getName(), isCustomer(authentication));
    }

    private boolean isCustomer(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER"));
    }
}
