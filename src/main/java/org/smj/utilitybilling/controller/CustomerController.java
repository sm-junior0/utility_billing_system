package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.CustomerRequest;
import org.smj.utilitybilling.dto.request.UserStatusUpdateRequest;
import org.smj.utilitybilling.dto.response.CustomerResponse;
import org.smj.utilitybilling.dto.response.PageResponse;
import org.smj.utilitybilling.service.CustomerService;
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
 * REST controller exposing Customer API endpoints.
 */

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "03 - Customer Setup", description = "Customer registration records, owner-user links, and lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "03 - Customer Setup",
            summary = "Step 6: Admin creates a customer record",
            description = "Protected endpoint. ROLE_ADMIN creates the business customer profile, prevents duplicate National ID registration, and can link the customer to a ROLE_CUSTOMER user through ownerUserId.")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR')")
    @Operation(
            tags = "03 - Customer Setup",
            summary = "List customers for staff users (paginated)",
            description = "Protected endpoint. ROLE_ADMIN, ROLE_FINANCE, and ROLE_OPERATOR can list customers to support setup, reading capture, billing, and payment review. Query params: page (0-based), size (default 10), sort (allowed: id, fullNames, email, nationalId, status, createdAt — e.g. sort=fullNames,asc).")
    public PageResponse<CustomerResponse> getAllCustomers(
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return customerService.getAllCustomers(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'OPERATOR', 'CUSTOMER')")
    @Operation(
            tags = "09 - Customer Self-Service",
            summary = "View one customer profile",
            description = "Protected endpoint. Staff can view a customer by ID. ROLE_CUSTOMER can only view the profile linked to their own user account.")
    public CustomerResponse getCustomer(@Parameter(description = "Customer ID to retrieve", required = true, example = "1") @PathVariable Long id, Authentication authentication) {
        return customerService.getCustomer(id, authentication.getName(), isCustomer(authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "03 - Customer Setup",
            summary = "Admin updates customer details",
            description = "Protected endpoint. ROLE_ADMIN updates customer identity, contact details, address, status value, and owner-user link.")
    public CustomerResponse updateCustomer(@Parameter(description = "Customer ID to update", required = true, example = "1") @PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.updateCustomer(id, request);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            tags = "03 - Customer Setup",
            summary = "Admin changes customer status",
            description = "Protected endpoint. ROLE_ADMIN activates or deactivates a customer. Inactive customers cannot receive generated bills.")
    public CustomerResponse updateStatus(@Parameter(description = "Customer ID whose status should be changed", required = true, example = "1") @PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest request) {
        return customerService.updateStatus(id, request.getStatus());
    }

    private boolean isCustomer(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER"));
    }
}
