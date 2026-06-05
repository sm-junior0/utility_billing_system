package org.smj.utilitybilling.repository;

import org.smj.utilitybilling.entity.Meter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for Meter persistence.
 */

public interface MeterRepository extends JpaRepository<Meter, Long> {

    boolean existsByMeterNumber(String meterNumber);

    Page<Meter> findByCustomerId(Long customerId, Pageable pageable);
}
