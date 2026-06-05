package org.smj.utilitybilling.dto.response;

import java.time.LocalDateTime;
/**
 * Response DTO returned by Signup APIs.
 */

public record SignupResponse(
        Long userId,
        String email,
        String message,
        LocalDateTime timestamp
) {
}
