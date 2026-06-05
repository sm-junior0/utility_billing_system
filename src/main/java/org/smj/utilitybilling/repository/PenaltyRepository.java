package org.smj.utilitybilling.repository;

import java.util.Optional;
import org.smj.utilitybilling.entity.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for Penalty persistence.
 */

public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    Optional<Penalty> findFirstByActiveTrueOrderByIdDesc();
}
