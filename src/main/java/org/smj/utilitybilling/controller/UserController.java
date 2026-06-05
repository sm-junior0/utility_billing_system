package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.UserRoleUpdateRequest;
import org.smj.utilitybilling.dto.request.UserStatusUpdateRequest;
import org.smj.utilitybilling.dto.response.PageResponse;
import org.smj.utilitybilling.dto.response.UserResponse;
import org.smj.utilitybilling.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * REST controller exposing User API endpoints.
 */

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "02 - Admin User Management", description = "ROLE_ADMIN APIs for user review, status management, and role assignment")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(
            tags = "02 - Admin User Management",
            summary = "Step 4: Admin lists registered users (paginated)",
            description = "Protected endpoint. ROLE_ADMIN reviews signup accounts with pagination. Query params: page (0-based), size (default 10), sort (allowed: id, fullNames, email, phoneNumber, status, role, createdAt, updatedAt — e.g. sort=email,desc). Leave sort empty to default to id,asc.")
    public PageResponse<UserResponse> getUsers(
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return userService.getUsers(pageable);
    }

    @GetMapping("/{id}")
    @Operation(
            tags = "02 - Admin User Management",
            summary = "Admin gets one user profile",
            description = "Protected endpoint. ROLE_ADMIN retrieves a single user account by database ID to inspect role, status, email, and contact information.")
    public UserResponse getUser(@Parameter(description = "User account ID from the users list", required = true, example = "2") @PathVariable Long id) {
        return userService.getUser(id);
    }

    @PutMapping("/{id}/status")
    @Operation(
            tags = "02 - Admin User Management",
            summary = "Admin changes user account status",
            description = "Protected endpoint. ROLE_ADMIN changes a user to ACTIVE, INACTIVE, or PENDING_VERIFICATION. Inactive or pending users cannot log in.")
    public UserResponse updateStatus(@Parameter(description = "User account ID whose status should be changed", required = true, example = "2") @PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest request) {
        return userService.updateStatus(id, request.getStatus());
    }

    @PutMapping("/{id}/role")
    @Operation(
            tags = "02 - Admin User Management",
            summary = "Step 5: Admin promotes or demotes a user role",
            description = "Protected endpoint. Only ROLE_ADMIN can assign ROLE_CUSTOMER, ROLE_OPERATOR, ROLE_FINANCE, or ROLE_ADMIN. The affected user must log in again to receive a new JWT containing the updated role.")
    public UserResponse updateRole(@Parameter(description = "User account ID whose role should be changed", required = true, example = "2") @PathVariable Long id, @Valid @RequestBody UserRoleUpdateRequest request) {
        return userService.updateRole(id, request.getRole());
    }
}
