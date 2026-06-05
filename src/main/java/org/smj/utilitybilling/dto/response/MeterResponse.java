package org.smj.utilitybilling.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.smj.utilitybilling.enums.MeterStatus;
import org.smj.utilitybilling.enums.MeterType;
/**
 * Response DTO returned by Meter APIs.
 */

public record MeterResponse(
        Long id,
        String meterNumber,
        MeterType meterType,
        LocalDate installationDate,
        MeterStatus status,
        Long customerId,
        String customerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
