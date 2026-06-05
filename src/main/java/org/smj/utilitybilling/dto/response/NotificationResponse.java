package org.smj.utilitybilling.dto.response;

import java.time.LocalDateTime;
import org.smj.utilitybilling.enums.NotificationStatus;
import org.smj.utilitybilling.enums.NotificationType;
/**
 * Response DTO returned by Notification APIs.
 */

public record NotificationResponse(
        Long id,
        Long customerId,
        String customerName,
        Long billId,
        String message,
        NotificationType type,
        NotificationStatus status,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
}
