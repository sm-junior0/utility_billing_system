package org.smj.utilitybilling.dto.response;

import java.time.LocalDateTime;
import org.smj.utilitybilling.enums.UserStatus;
/**
 * Response DTO returned by Customer APIs.
 */

public record CustomerResponse(
        Long id,
        String fullNames,
        String nationalId,
        String email,
        String phoneNumber,
        String address,
        UserStatus status,
        Long ownerUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
