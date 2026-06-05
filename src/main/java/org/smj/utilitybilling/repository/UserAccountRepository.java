package org.smj.utilitybilling.repository;

import java.util.Optional;
import org.smj.utilitybilling.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for UserAccount persistence.
 */

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
