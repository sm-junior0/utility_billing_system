package org.smj.utilitybilling.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.smj.utilitybilling.entity.Tariff;
import org.smj.utilitybilling.enums.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for Tariff persistence.
 */

public interface TariffRepository extends JpaRepository<Tariff, Long> {

    List<Tariff> findByActiveTrue();

    Optional<Tariff> findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToIsNullOrderByVersionDesc(
            MeterType meterType, LocalDate date);

    Optional<Tariff> findFirstByMeterTypeAndActiveTrueAndEffectiveFromLessThanEqualAndEffectiveToGreaterThanEqualOrderByVersionDesc(
            MeterType meterType, LocalDate startDate, LocalDate endDate);
}
