package org.smj.utilitybilling.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.response.NotificationResponse;
import org.smj.utilitybilling.dto.response.PageResponse;
import org.smj.utilitybilling.entity.Bill;
import org.smj.utilitybilling.enums.BillStatus;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.service.BillService;
import org.smj.utilitybilling.service.NotificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * REST controller exposing Notification API endpoints.
 */

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "10 - Notifications", description = "Notification audit, customer messages, and optional email sending")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final BillService billService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(
            tags = "10 - Notifications",
            summary = "Step 15: Review all generated notifications (paginated)",
            description = "Protected endpoint. ROLE_ADMIN and ROLE_FINANCE audit all notification messages created by database triggers during bill generation and full payment. Query params: page (0-based), size (default 10), sort (allowed: id, type, status, sentAt, createdAt — e.g. sort=createdAt,desc).")
    public PageResponse<NotificationResponse> getAllNotifications(
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return notificationService.getAllNotifications(pageable);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'CUSTOMER')")
    @Operation(
            tags = "09 - Customer Self-Service",
            summary = "View customer notifications (paginated)",
            description = "Protected endpoint. Staff can view notification history for any customer. ROLE_CUSTOMER can only view notifications belonging to their linked customer profile. Supports page, size, and sort query params.")
    public PageResponse<NotificationResponse> getNotificationsByCustomer(
            @Parameter(description = "Customer ID whose notifications should be listed", required = true, example = "1") @PathVariable Long customerId,
            Authentication authentication,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return notificationService.getNotificationsByCustomer(customerId, authentication.getName(), isCustomer(authentication), pageable);
    }

    @PostMapping("/bill/{billId}/send-payment-completed")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(
            tags = "10 - Notifications",
            summary = "Send payment-completed email for a paid bill",
            description = "Protected endpoint. Creates a PAYMENT_COMPLETED notification if missing, then emails the customer. Use when a bill is PAID but no payment email was received.")
    public NotificationResponse sendPaymentCompletedForBill(
            @Parameter(description = "Paid bill ID", required = true, example = "1") @PathVariable Long billId) {
        Bill bill = billService.getBillEntity(billId);
        if (bill.getStatus() != BillStatus.PAID) {
            throw new BadRequestException("Bill must be fully paid before sending a payment-completed email");
        }
        return notificationService.sendPaymentCompletedNotification(bill);
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(
            tags = "10 - Notifications",
            summary = "Send one notification by email",
            description = "Protected endpoint. ROLE_ADMIN or ROLE_FINANCE sends an existing stored notification using Java Mail Sender and updates the notification status.")
    public NotificationResponse sendNotification(@Parameter(description = "Notification ID to send by email", required = true, example = "1") @PathVariable Long id) {
        return notificationService.sendNotification(id);
    }

    private boolean isCustomer(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER"));
    }
}
