package org.smj.utilitybilling.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smj.utilitybilling.dto.response.NotificationResponse;
import org.smj.utilitybilling.entity.Bill;
import org.smj.utilitybilling.entity.Notification;
import org.smj.utilitybilling.enums.NotificationStatus;
import org.smj.utilitybilling.enums.NotificationType;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.NotificationRepository;
import org.smj.utilitybilling.util.ResponseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages notification records and delivers every notification to the customer by email.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final CurrentUserService currentUserService;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .map(ResponseMapper::toNotificationResponse)
                .toList();
    }

    public List<NotificationResponse> getNotificationsByCustomer(Long customerId, String email, boolean customerAccess) {
        if (customerAccess) {
            currentUserService.validateCustomerOwnership(customerId, email);
            return notificationRepository.findByCustomerOwnerUserEmail(email).stream()
                    .map(ResponseMapper::toNotificationResponse)
                    .toList();
        }
        return notificationRepository.findByCustomerId(customerId).stream()
                .map(ResponseMapper::toNotificationResponse)
                .toList();
    }

    @Transactional
    public NotificationResponse sendNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        deliverEmail(notification);
        return ResponseMapper.toNotificationResponse(notification);
    }

    @Transactional
    public NotificationResponse sendBillGeneratedNotification(Bill bill) {
        Notification notification = notificationRepository
                .findTopByBillIdAndTypeOrderByCreatedAtDesc(bill.getId(), NotificationType.BILL_GENERATED)
                .orElseGet(() -> notificationRepository.save(buildBillGeneratedNotification(bill)));
        deliverEmail(notification);
        return ResponseMapper.toNotificationResponse(notification);
    }

    @Transactional
    public NotificationResponse sendPaymentCompletedNotification(Bill bill) {
        Notification notification = notificationRepository
                .findTopByBillIdAndTypeOrderByCreatedAtDesc(bill.getId(), NotificationType.PAYMENT_COMPLETED)
                .orElseGet(() -> notificationRepository.save(buildPaymentCompletedNotification(bill)));
        deliverEmail(notification);
        return ResponseMapper.toNotificationResponse(notification);
    }

    /**
     * Sends email for any notification still marked PENDING (for example rows created only by DB triggers).
     */
    @Transactional
    public void sendAllPendingNotificationEmails() {
        List<Notification> pending = notificationRepository.findByStatus(NotificationStatus.PENDING);
        log.info("Sending {} pending notification email(s)", pending.size());
        pending.forEach(this::deliverEmail);
    }

    private Notification buildBillGeneratedNotification(Bill bill) {
        return Notification.builder()
                .customer(bill.getCustomer())
                .bill(bill)
                .message(buildBillGeneratedMessage(bill))
                .type(NotificationType.BILL_GENERATED)
                .status(NotificationStatus.PENDING)
                .build();
    }

    private Notification buildPaymentCompletedNotification(Bill bill) {
        return Notification.builder()
                .customer(bill.getCustomer())
                .bill(bill)
                .message(buildPaymentCompletedMessage(bill))
                .type(NotificationType.PAYMENT_COMPLETED)
                .status(NotificationStatus.PENDING)
                .build();
    }

    static String buildBillGeneratedMessage(Bill bill) {
        return "Dear " + bill.getCustomer().getFullNames() + ",\nYour "
                + String.format("%02d", bill.getBillingMonth()) + "/" + bill.getBillingYear()
                + " utility bill of " + bill.getTotalAmount() + " FRW has been successfully processed.";
    }

    static String buildPaymentCompletedMessage(Bill bill) {
        return "Dear " + bill.getCustomer().getFullNames() + ",\nYour "
                + String.format("%02d", bill.getBillingMonth()) + "/" + bill.getBillingYear()
                + " utility bill of " + bill.getTotalAmount() + " FRW has been successfully paid.";
    }

    private void deliverEmail(Notification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailUsername != null && !mailUsername.isBlank()) {
                message.setFrom(mailUsername);
            }
            message.setTo(notification.getCustomer().getEmail());
            message.setSubject(resolveEmailSubject(notification.getType()));
            message.setText(notification.getMessage());
            mailSender.send(message);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("Notification email sent id={} type={} to={}", notification.getId(), notification.getType(),
                    notification.getCustomer().getEmail());
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            log.error("Notification email failed id={} type={} to={} reason={}", notification.getId(),
                    notification.getType(), notification.getCustomer().getEmail(), ex.getMessage());
        } finally {
            notificationRepository.save(notification);
        }
    }

    private String resolveEmailSubject(NotificationType type) {
        return switch (type) {
            case BILL_GENERATED -> "Utility Billing - Bill Generated";
            case PAYMENT_COMPLETED -> "Utility Billing - Payment Completed";
            case GENERAL -> "Utility Billing Notification";
        };
    }
}
