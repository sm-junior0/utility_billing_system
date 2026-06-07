package org.smj.utilitybilling.repository;

import java.util.List;
import java.util.Optional;
import org.smj.utilitybilling.entity.OtpVerification;
import org.smj.utilitybilling.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Spring Data repository for OtpVerification persistence.
 */

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    List<OtpVerification> findByEmailAndPurposeAndUsedFalse(String email, OtpPurpose purpose);
}
