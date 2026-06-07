package org.smj.utilitybilling.service;

import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smj.utilitybilling.dto.request.MeterReadingRequest;
import org.smj.utilitybilling.dto.response.MeterReadingResponse;
import org.smj.utilitybilling.dto.response.PageResponse;
import org.smj.utilitybilling.entity.Meter;
import org.smj.utilitybilling.entity.MeterReading;
import org.smj.utilitybilling.entity.UserAccount;
import org.smj.utilitybilling.enums.MeterStatus;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.MeterReadingRepository;
import org.smj.utilitybilling.util.PageableUtils;
import org.smj.utilitybilling.util.ResponseMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing MeterReading business rules.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterReadingService {

    private static final Set<String> ALLOWED_READING_SORT_FIELDS = Set.of(
            "id", "previousReading", "currentReading", "consumption",
            "readingDate", "readingMonth", "readingYear", "createdAt", "updatedAt");

    private final MeterReadingRepository meterReadingRepository;
    private final MeterService meterService;
    private final CurrentUserService currentUserService;

    @Transactional
    public MeterReadingResponse createReading(MeterReadingRequest request, String operatorEmail) {
        Meter meter = meterService.getMeterEntity(request.getMeterId());
        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BadRequestException("Only active meters can receive readings");
        }
        if (request.getReadingDate().isBefore(meter.getInstallationDate())) {
            throw new BadRequestException("Reading date cannot be before the meter installation date");
        }
        if (request.getCurrentReading().compareTo(request.getPreviousReading()) <= 0) {
            throw new BadRequestException("Current reading must be greater than previous reading");
        }

        Integer month = request.getReadingDate().getMonthValue();
        Integer year = request.getReadingDate().getYear();
        if (meterReadingRepository.existsByMeterAndReadingMonthAndReadingYear(meter, month, year)) {
            throw new BadRequestException("Only one reading per meter per month and year is allowed");
        }

        meterReadingRepository.findTopByMeterOrderByReadingDateDescIdDesc(meter).ifPresent(lastReading -> {
            if (lastReading.getCurrentReading().compareTo(request.getPreviousReading()) != 0) {
                throw new BadRequestException("Previous reading must match the last recorded current reading");
            }
        });

        BigDecimal consumption = request.getCurrentReading().subtract(request.getPreviousReading());
        UserAccount operator = currentUserService.getUserByEmail(operatorEmail);
        MeterReading reading = MeterReading.builder()
                .meter(meter)
                .previousReading(request.getPreviousReading())
                .currentReading(request.getCurrentReading())
                .consumption(consumption)
                .readingDate(request.getReadingDate())
                .readingMonth(month)
                .readingYear(year)
                .capturedBy(operator)
                .build();
        return ResponseMapper.toMeterReadingResponse(meterReadingRepository.save(reading));
    }

    public PageResponse<MeterReadingResponse> getAllReadings(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        Page<MeterReadingResponse> page = meterReadingRepository.findAll(safePageable)
                .map(ResponseMapper::toMeterReadingResponse);
        log.info("Listed readings page={} size={} totalElements={}", page.getNumber(), page.getSize(), page.getTotalElements());
        return PageResponse.from(page);
    }

    public PageResponse<MeterReadingResponse> getReadingsByMeter(Long meterId, Pageable pageable) {
        meterService.getMeterEntity(meterId);
        Pageable safePageable = sanitizePageable(pageable);
        Page<MeterReadingResponse> page = meterReadingRepository.findByMeterId(meterId, safePageable)
                .map(ResponseMapper::toMeterReadingResponse);
        log.info("Listed readings for meterId={} page={} size={} totalElements={}",
                meterId, page.getNumber(), page.getSize(), page.getTotalElements());
        return PageResponse.from(page);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        return PageableUtils.withDefaultSort(
                PageableUtils.validateSort(pageable, ALLOWED_READING_SORT_FIELDS, "id"), "id");
    }

    public MeterReading getReadingEntity(Long id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found"));
    }
}
