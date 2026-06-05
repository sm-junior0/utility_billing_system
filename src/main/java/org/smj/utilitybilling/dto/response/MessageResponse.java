package org.smj.utilitybilling.dto.response;

import java.time.LocalDateTime;
/**
 * Response DTO returned by Message APIs.
 */

public record MessageResponse(String message, LocalDateTime timestamp) {

    public static MessageResponse of(String message) {
        return new MessageResponse(message, LocalDateTime.now());
    }
}
