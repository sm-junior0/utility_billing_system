package org.smj.utilitybilling.dto.response;

import java.time.LocalDateTime;
import org.smj.utilitybilling.enums.Role;
import org.smj.utilitybilling.enums.UserStatus;
/**
 * Response DTO returned by User APIs.
 */

public record UserResponse(
        Long id,
        String fullNames,
        String email,
        String phoneNumber,
        UserStatus status,
        Role role,
        Long customerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
