package org.smj.utilitybilling.repository;

import java.util.List;
import java.util.Optional;
import org.smj.utilitybilling.entity.Notification;
import org.smj.utilitybilling.enums.NotificationStatus;
import org.smj.utilitybilling.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for customer notification records created by triggers and services.
 */

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByCustomerId(Long customerId);

    List<Notification> findByCustomerOwnerUserEmail(String email);

    Optional<Notification> findTopByBillIdAndTypeOrderByCreatedAtDesc(Long billId, NotificationType type);

    List<Notification> findByStatus(NotificationStatus status);
}
