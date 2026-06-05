package org.smj.utilitybilling.repository;

import java.util.List;
import java.util.Optional;
import org.smj.utilitybilling.entity.Notification;
import org.smj.utilitybilling.enums.NotificationStatus;
import org.smj.utilitybilling.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for customer notification records created by triggers and services.
 */

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByCustomerId(Long customerId, Pageable pageable);

    Page<Notification> findByCustomerOwnerUserEmail(String email, Pageable pageable);

    Optional<Notification> findTopByBillIdAndTypeOrderByCreatedAtDesc(Long billId, NotificationType type);

    List<Notification> findByStatus(NotificationStatus status);
}
