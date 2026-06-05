package org.smj.utilitybilling.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.TaxRequest;
import org.smj.utilitybilling.dto.response.TaxResponse;
import org.smj.utilitybilling.entity.Tax;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.TaxRepository;
import org.smj.utilitybilling.util.ResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing Tax business rules.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxService {

    private final TaxRepository taxRepository;

    @Transactional
    public TaxResponse createTax(TaxRequest request) {
        if (request.getActive() == null || request.getActive()) {
            taxRepository.findAll().forEach(tax -> tax.setActive(false));
        }
        Tax tax = Tax.builder()
                .name(request.getName())
                .percentage(request.getPercentage())
                .active(request.getActive() == null || request.getActive())
                .build();
        return ResponseMapper.toTaxResponse(taxRepository.save(tax));
    }

    public List<TaxResponse> getAllTaxes() {
        return taxRepository.findAll().stream()
                .map(ResponseMapper::toTaxResponse)
                .toList();
    }

    @Transactional
    public TaxResponse updateStatus(Long id, boolean active) {
        if (active) {
            taxRepository.findAll().forEach(tax -> tax.setActive(false));
        }
        Tax tax = taxRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax not found"));
        tax.setActive(active);
        return ResponseMapper.toTaxResponse(tax);
    }

    public Tax getActiveTax() {
        return taxRepository.findFirstByActiveTrueOrderByIdDesc()
                .orElseThrow(() -> new ResourceNotFoundException("No active tax is configured"));
    }
}
