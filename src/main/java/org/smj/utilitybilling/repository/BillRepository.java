package org.smj.utilitybilling.repository;

import java.util.List;
import java.util.Optional;
import org.smj.utilitybilling.entity.Bill;
import org.smj.utilitybilling.enums.BillStatus;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for Bill persistence.
 */

public interface BillRepository extends JpaRepository<Bill, Long> {

    boolean existsByMeterIdAndBillingMonthAndBillingYear(Long meterId, Integer billingMonth, Integer billingYear);

    List<Bill> findByCustomerId(Long customerId);

    List<Bill> findByCustomerOwnerUserEmail(String email);

    List<Bill> findByStatusIn(List<BillStatus> statuses);

    Optional<Bill> findByBillReference(String billReference);
}
