package org.smj.utilitybilling.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * Response DTO returned by MeterReading APIs.
 */

public record MeterReadingResponse(
        Long id,
        Long meterId,
        String meterNumber,
        BigDecimal previousReading,
        BigDecimal currentReading,
        BigDecimal consumption,
        LocalDate readingDate,
        Integer readingMonth,
        Integer readingYear,
        String capturedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
