package org.smj.utilitybilling.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.TariffRequest;
import org.smj.utilitybilling.dto.request.TariffTierRequest;
import org.smj.utilitybilling.dto.response.TariffResponse;
import org.smj.utilitybilling.entity.Tariff;
import org.smj.utilitybilling.entity.TariffTier;
import org.smj.utilitybilling.enums.MeterType;
import org.smj.utilitybilling.enums.TariffType;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.TariffRepository;
import org.smj.utilitybilling.repository.TariffTierRepository;
import org.smj.utilitybilling.util.ResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing Tariff business rules.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TariffService {

    private final TariffRepository tariffRepository;
    private final TariffTierRepository tariffTierRepository;

    @Transactional
    public TariffResponse createTariff(TariffRequest request) {
        validateTariffRequest(request);

        Tariff tariff = Tariff.builder()
                .meterType(request.getMeterType())
                .tariffType(request.getTariffType())
                .version(request.getVersion())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .active(true)
                .flatUnitPrice(request.getFlatUnitPrice())
                .build();
        Tariff savedTariff = tariffRepository.save(tariff);

        if (request.getTariffType() == TariffType.TIERED) {
            saveTariffTiers(savedTariff, request.getTiers());
        }
        return ResponseMapper.toTariffResponse(savedTariff, tariffTierRepository.findByTariffIdOrderByMinUnitsAsc(savedTariff.getId()));
    }

    public List<TariffResponse> getAllTariffs() {
        return tariffRepository.findAll().stream()
                .map(tariff -> ResponseMapper.toTariffResponse(tariff, tariffTierRepository.findByTariffIdOrderByMinUnitsAsc(tariff.getId())))
                .toList();
    }

    @Transactional
    public TariffResponse deactivateTariff(Long id) {
        Tariff tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff not found"));
        tariff.setActive(false);
        return ResponseMapper.toTariffResponse(tariff, tariffTierRepository.findByTariffIdOrderByMinUnitsAsc(tariff.getId()));
    }

    public Optional<Tariff> findApplicableTariff(MeterType meterType, LocalDate billingDate) {
        return tariffRepository
                .findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByVersionDesc(
                        meterType, billingDate, billingDate)
                .or(() -> tariffRepository.findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByVersionDesc(
                        meterType, billingDate));
    }

    public Tariff getApplicableTariff(MeterType meterType, LocalDate billingDate) {
        return findApplicableTariff(meterType, billingDate)
                .orElseThrow(() -> new ResourceNotFoundException("No active tariff found for the selected meter type"));
    }

    public List<TariffTier> getTariffTiers(Long tariffId) {
        return tariffTierRepository.findByTariffIdOrderByMinUnitsAsc(tariffId);
    }

    private void saveTariffTiers(Tariff tariff, List<TariffTierRequest> tiers) {
        for (TariffTierRequest tierRequest : tiers) {
            TariffTier tier = TariffTier.builder()
                    .tariff(tariff)
                    .minUnits(tierRequest.getMinUnits())
                    .maxUnits(tierRequest.getMaxUnits())
                    .pricePerUnit(tierRequest.getPricePerUnit())
                    .build();
            tariffTierRepository.save(tier);
        }
    }

    private void validateTariffRequest(TariffRequest request) {
        if (request.getTariffType() == TariffType.FLAT && request.getFlatUnitPrice() == null) {
            throw new BadRequestException("Flat tariffs require a flat unit price");
        }
        if (request.getTariffType() == TariffType.TIERED && (request.getTiers() == null || request.getTiers().isEmpty())) {
            throw new BadRequestException("Tiered tariffs require at least one tier");
        }
        if (request.getEffectiveTo() != null && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new BadRequestException("Effective to date cannot be before effective from date");
        }
    }
}
