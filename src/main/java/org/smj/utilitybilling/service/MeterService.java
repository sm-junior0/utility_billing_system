package org.smj.utilitybilling.service;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smj.utilitybilling.dto.request.MeterRequest;
import org.smj.utilitybilling.dto.response.MeterResponse;
import org.smj.utilitybilling.dto.response.PageResponse;
import org.smj.utilitybilling.entity.Customer;
import org.smj.utilitybilling.entity.Meter;
import org.smj.utilitybilling.enums.MeterStatus;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.MeterRepository;
import org.smj.utilitybilling.util.PageableUtils;
import org.smj.utilitybilling.util.ResponseMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing Meter business rules.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterService {

    private static final Set<String> ALLOWED_METER_SORT_FIELDS = Set.of(
            "id", "meterNumber", "meterType", "installationDate", "status", "createdAt", "updatedAt");

    private final MeterRepository meterRepository;
    private final CustomerService customerService;
    private final CurrentUserService currentUserService;

    @Transactional
    public MeterResponse createMeter(MeterRequest request) {
        if (meterRepository.existsByMeterNumber(request.getMeterNumber())) {
            throw new BadRequestException("Meter number is already registered");
        }
        Customer customer = customerService.getCustomerEntity(request.getCustomerId());
        Meter meter = Meter.builder()
                .meterNumber(request.getMeterNumber())
                .meterType(request.getMeterType())
                .installationDate(request.getInstallationDate())
                .status(request.getStatus() == null ? MeterStatus.ACTIVE : request.getStatus())
                .customer(customer)
                .build();
        return ResponseMapper.toMeterResponse(meterRepository.save(meter));
    }

    public PageResponse<MeterResponse> getAllMeters(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        Page<MeterResponse> page = meterRepository.findAll(safePageable)
                .map(ResponseMapper::toMeterResponse);
        log.info("Listed meters page={} size={} totalElements={}", page.getNumber(), page.getSize(), page.getTotalElements());
        return PageResponse.from(page);
    }

    public PageResponse<MeterResponse> getMetersByCustomer(Long customerId, String email, boolean customerAccess, Pageable pageable) {
        if (customerAccess) {
            currentUserService.validateCustomerOwnership(customerId, email);
        }
        customerService.getCustomerEntity(customerId);
        Pageable safePageable = sanitizePageable(pageable);
        Page<MeterResponse> page = meterRepository.findByCustomerId(customerId, safePageable)
                .map(ResponseMapper::toMeterResponse);
        log.info("Listed meters for customerId={} page={} size={} totalElements={}",
                customerId, page.getNumber(), page.getSize(), page.getTotalElements());
        return PageResponse.from(page);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        return PageableUtils.withDefaultSort(
                PageableUtils.validateSort(pageable, ALLOWED_METER_SORT_FIELDS, "id"), "id");
    }

    @Transactional
    public MeterResponse updateStatus(Long id, MeterStatus status) {
        Meter meter = getMeterEntity(id);
        meter.setStatus(status);
        return ResponseMapper.toMeterResponse(meter);
    }

    public Meter getMeterEntity(Long id) {
        return meterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found"));
    }
}
