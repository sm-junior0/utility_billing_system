package org.smj.utilitybilling.repository;

import java.util.List;
import java.util.Optional;
import org.smj.utilitybilling.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for OtpVerification persistence.
 */

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    List<OtpVerification> findByEmailAndUsedFalse(String email);
}
