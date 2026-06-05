package org.smj.utilitybilling.repository;

import java.util.List;
import org.smj.utilitybilling.entity.PrepaidTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for PrepaidTransaction persistence.
 */

public interface PrepaidTransactionRepository extends JpaRepository<PrepaidTransaction, Long> {

    List<PrepaidTransaction> findByCustomerId(Long customerId);

    List<PrepaidTransaction> findByCustomerOwnerUserEmail(String email);
}
