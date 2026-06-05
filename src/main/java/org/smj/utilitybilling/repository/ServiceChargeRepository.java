package org.smj.utilitybilling.repository;

import java.util.Optional;
import org.smj.utilitybilling.entity.ServiceCharge;
import org.smj.utilitybilling.enums.MeterType;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for ServiceCharge persistence.
 */

public interface ServiceChargeRepository extends JpaRepository<ServiceCharge, Long> {

    Optional<ServiceCharge> findFirstByMeterTypeAndActiveTrueOrderByIdDesc(MeterType meterType);
}
