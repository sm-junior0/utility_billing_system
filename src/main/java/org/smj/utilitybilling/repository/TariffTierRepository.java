package org.smj.utilitybilling.repository;

import java.util.List;
import org.smj.utilitybilling.entity.TariffTier;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for TariffTier persistence.
 */

public interface TariffTierRepository extends JpaRepository<TariffTier, Long> {

    List<TariffTier> findByTariffIdOrderByMinUnitsAsc(Long tariffId);
}
