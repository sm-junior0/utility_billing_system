package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.BillGenerationRequest;
import org.smj.utilitybilling.dto.response.BillResponse;
import org.smj.utilitybilling.service.BillService;
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
 * REST controller exposing Bill API endpoints.
 */

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@Tag(name = "07 - Billing Operations", description = "Bill generation, approval, balance tracking, and bill lookup")
@SecurityRequirement(name = "bearerAuth")
public class BillController {

    private final BillService billService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(
            tags = "07 - Billing Operations",
            summary = "Step 12: Generate a monthly utility bill",
            description = "Protected endpoint. ROLE_FINANCE or ROLE_ADMIN generates a bill from a captured meter reading using active tariff, tax, penalty, and service charge configuration. Inactive customers cannot receive bills. PostgreSQL trigger creates a notification message after bill insertion.")
    public ResponseEntity<BillResponse> generateBill(@Valid @RequestBody BillGenerationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.generateBill(request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(
            tags = "07 - Billing Operations",
            summary = "Step 13: Approve a pending bill",
            description = "Protected endpoint. ROLE_FINANCE or ROLE_ADMIN approves a generated bill and records approvedBy and approvedAt audit fields.")
    public BillResponse approveBill(@Parameter(description = "Bill ID to approve", required = true, example = "1") @PathVariable Long id, Authentication authentication) {
        return billService.approveBill(id, authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(
            tags = "07 - Billing Operations",
            summary = "List all bills for finance review",
            description = "Protected endpoint. ROLE_ADMIN and ROLE_FINANCE can review all bills, statuses, due dates, approved fields, paid amounts, and outstanding balances.")
    public List<BillResponse> getAllBills() {
        return billService.getAllBills();
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(
            tags = "09 - Customer Self-Service",
            summary = "View bills for a customer",
            description = "Protected endpoint. Staff can view bills for any customer. ROLE_CUSTOMER can only view bills belonging to their linked customer profile.")
    public List<BillResponse> getBillsByCustomer(@Parameter(description = "Customer ID whose bills should be listed", required = true, example = "1") @PathVariable Long customerId, Authentication authentication) {
        return billService.getBillsByCustomer(customerId, authentication.getName(), isCustomer(authentication));
    }

    @PostMapping("/apply-late-penalties")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(
            tags = "07 - Billing Operations",
            summary = "Apply late payment penalties to overdue bills",
            description = "Protected endpoint. Executes the PostgreSQL cursor_apply_late_penalties() stored procedure for approved or partially paid bills past their due date.")
    public ResponseEntity<Void> applyLatePenalties() {
        billService.applyLatePenalties();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(
            tags = "09 - Customer Self-Service",
            summary = "View one bill by ID",
            description = "Protected endpoint. Staff can view any bill by ID. ROLE_CUSTOMER can only view a bill that belongs to their linked customer profile.")
    public BillResponse getBill(@Parameter(description = "Bill ID to retrieve", required = true, example = "1") @PathVariable Long id, Authentication authentication) {
        return billService.getBill(id, authentication.getName(), isCustomer(authentication));
    }

    private boolean isCustomer(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER"));
    }
}
