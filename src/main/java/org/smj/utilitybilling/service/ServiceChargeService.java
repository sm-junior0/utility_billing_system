package org.smj.utilitybilling.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.ServiceChargeRequest;
import org.smj.utilitybilling.dto.response.ServiceChargeResponse;
import org.smj.utilitybilling.entity.ServiceCharge;
import org.smj.utilitybilling.enums.MeterType;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.ServiceChargeRepository;
import org.smj.utilitybilling.util.ResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing ServiceCharge business rules.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceChargeService {

    private final ServiceChargeRepository serviceChargeRepository;

    @Transactional
    public ServiceChargeResponse createServiceCharge(ServiceChargeRequest request) {
        if (request.getActive() == null || request.getActive()) {
            serviceChargeRepository.findAll().stream()
                    .filter(charge -> charge.getMeterType() == request.getMeterType())
                    .forEach(charge -> charge.setActive(false));
        }
        ServiceCharge charge = ServiceCharge.builder()
                .meterType(request.getMeterType())
                .amount(request.getAmount())
                .active(request.getActive() == null || request.getActive())
                .build();
        return ResponseMapper.toServiceChargeResponse(serviceChargeRepository.save(charge));
    }

    public List<ServiceChargeResponse> getAllServiceCharges() {
        return serviceChargeRepository.findAll().stream()
                .map(ResponseMapper::toServiceChargeResponse)
                .toList();
    }

    @Transactional
    public ServiceChargeResponse updateStatus(Long id, boolean active) {
        ServiceCharge charge = serviceChargeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service charge not found"));
        if (active) {
            serviceChargeRepository.findAll().stream()
                    .filter(existing -> existing.getMeterType() == charge.getMeterType())
                    .forEach(existing -> existing.setActive(false));
        }
        charge.setActive(active);
        return ResponseMapper.toServiceChargeResponse(charge);
    }

    public ServiceCharge getActiveCharge(MeterType meterType) {
        return serviceChargeRepository.findFirstByMeterTypeAndActiveTrueOrderByIdDesc(meterType)
                .orElseThrow(() -> new ResourceNotFoundException("No active service charge found for meter type " + meterType));
    }
}
