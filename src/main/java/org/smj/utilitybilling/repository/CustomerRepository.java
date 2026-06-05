package org.smj.utilitybilling.repository;

import java.util.Optional;
import org.smj.utilitybilling.entity.Customer;
import org.smj.utilitybilling.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for Customer persistence.
 */

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByNationalId(String nationalId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<Customer> findByOwnerUser(UserAccount ownerUser);

    Optional<Customer> findByOwnerUserId(Long ownerUserId);

    Optional<Customer> findByOwnerUserEmail(String email);
}
