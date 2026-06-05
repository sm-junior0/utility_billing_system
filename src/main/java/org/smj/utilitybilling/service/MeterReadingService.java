package org.smj.utilitybilling.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.MeterReadingRequest;
import org.smj.utilitybilling.dto.response.MeterReadingResponse;
import org.smj.utilitybilling.entity.Meter;
import org.smj.utilitybilling.entity.MeterReading;
import org.smj.utilitybilling.entity.UserAccount;
import org.smj.utilitybilling.enums.MeterStatus;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.MeterReadingRepository;
import org.smj.utilitybilling.util.ResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing MeterReading business rules.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final MeterService meterService;
    private final CurrentUserService currentUserService;

    @Transactional
    public MeterReadingResponse createReading(MeterReadingRequest request, String operatorEmail) {
        Meter meter = meterService.getMeterEntity(request.getMeterId());
        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BadRequestException("Only active meters can receive readings");
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

    public List<MeterReadingResponse> getAllReadings() {
        return meterReadingRepository.findAll().stream()
                .map(ResponseMapper::toMeterReadingResponse)
                .toList();
    }

    public List<MeterReadingResponse> getReadingsByMeter(Long meterId) {
        meterService.getMeterEntity(meterId);
        return meterReadingRepository.findByMeterId(meterId).stream()
                .map(ResponseMapper::toMeterReadingResponse)
                .toList();
    }

    public MeterReading getReadingEntity(Long id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter reading not found"));
    }
}
