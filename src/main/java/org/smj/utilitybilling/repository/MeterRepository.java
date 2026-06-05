package org.smj.utilitybilling.repository;

import java.util.List;
import org.smj.utilitybilling.entity.Customer;
import org.smj.utilitybilling.entity.Meter;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for Meter persistence.
 */

public interface MeterRepository extends JpaRepository<Meter, Long> {

    boolean existsByMeterNumber(String meterNumber);

    List<Meter> findByCustomer(Customer customer);
}
