package org.smj.utilitybilling.repository;

import java.util.List;
import org.smj.utilitybilling.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for Payment persistence.
 */

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBillId(Long billId);

    List<Payment> findByBillCustomerId(Long customerId);

    List<Payment> findByBillCustomerOwnerUserEmail(String email);
}
