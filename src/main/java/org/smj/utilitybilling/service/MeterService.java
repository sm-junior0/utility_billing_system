package org.smj.utilitybilling.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.MeterRequest;
import org.smj.utilitybilling.dto.response.MeterResponse;
import org.smj.utilitybilling.entity.Customer;
import org.smj.utilitybilling.entity.Meter;
import org.smj.utilitybilling.enums.MeterStatus;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.MeterRepository;
import org.smj.utilitybilling.util.ResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing Meter business rules.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterService {

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

    public List<MeterResponse> getAllMeters() {
        return meterRepository.findAll().stream()
                .map(ResponseMapper::toMeterResponse)
                .toList();
    }

    public List<MeterResponse> getMetersByCustomer(Long customerId, String email, boolean customerAccess) {
        if (customerAccess) {
            currentUserService.validateCustomerOwnership(customerId, email);
        }
        Customer customer = customerService.getCustomerEntity(customerId);
        return meterRepository.findByCustomer(customer).stream()
                .map(ResponseMapper::toMeterResponse)
                .toList();
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
