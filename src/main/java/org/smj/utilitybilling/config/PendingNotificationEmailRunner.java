package org.smj.utilitybilling.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smj.utilitybilling.service.NotificationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * On startup, emails any notification rows still marked PENDING so no message is left undelivered.
 */

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class PendingNotificationEmailRunner implements CommandLineRunner {

    private final NotificationService notificationService;

    @Override
    public void run(String... args) {
        notificationService.sendAllPendingNotificationEmails();
    }
}
