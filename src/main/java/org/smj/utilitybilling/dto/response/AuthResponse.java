package org.smj.utilitybilling.dto.response;

import org.smj.utilitybilling.enums.Role;
/**
 * Response DTO returned by Auth APIs.
 */

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String email,
        Role role
) {
}
