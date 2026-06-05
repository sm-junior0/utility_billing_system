package org.smj.utilitybilling.repository;

import java.util.Optional;
import org.smj.utilitybilling.entity.Meter;
import org.smj.utilitybilling.entity.MeterReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for MeterReading persistence.
 */

public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {

    boolean existsByMeterAndReadingMonthAndReadingYear(Meter meter, Integer readingMonth, Integer readingYear);

    Optional<MeterReading> findTopByMeterOrderByReadingDateDescIdDesc(Meter meter);

    Page<MeterReading> findByMeterId(Long meterId, Pageable pageable);
}
