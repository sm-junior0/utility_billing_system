package org.smj.utilitybilling.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.PenaltyRequest;
import org.smj.utilitybilling.dto.response.PenaltyResponse;
import org.smj.utilitybilling.entity.Penalty;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.PenaltyRepository;
import org.smj.utilitybilling.util.ResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing Penalty business rules.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PenaltyService {

    private final PenaltyRepository penaltyRepository;

    @Transactional
    public PenaltyResponse createPenalty(PenaltyRequest request) {
        if (request.getActive() == null || request.getActive()) {
            penaltyRepository.findAll().forEach(penalty -> penalty.setActive(false));
        }
        Penalty penalty = Penalty.builder()
                .name(request.getName())
                .percentage(request.getPercentage())
                .active(request.getActive() == null || request.getActive())
                .build();
        return ResponseMapper.toPenaltyResponse(penaltyRepository.save(penalty));
    }

    public List<PenaltyResponse> getAllPenalties() {
        return penaltyRepository.findAll().stream()
                .map(ResponseMapper::toPenaltyResponse)
                .toList();
    }

    @Transactional
    public PenaltyResponse updateStatus(Long id, boolean active) {
        if (active) {
            penaltyRepository.findAll().forEach(penalty -> penalty.setActive(false));
        }
        Penalty penalty = penaltyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty not found"));
        penalty.setActive(active);
        return ResponseMapper.toPenaltyResponse(penalty);
    }

    public Penalty getActivePenaltyOrNull() {
        return penaltyRepository.findFirstByActiveTrueOrderByIdDesc().orElse(null);
    }
}
