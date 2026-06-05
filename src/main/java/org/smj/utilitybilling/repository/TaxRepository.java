package org.smj.utilitybilling.repository;

import java.util.Optional;
import org.smj.utilitybilling.entity.Tax;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for Tax persistence.
 */

public interface TaxRepository extends JpaRepository<Tax, Long> {

    Optional<Tax> findFirstByActiveTrueOrderByIdDesc();
}
