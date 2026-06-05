package org.smj.utilitybilling.config;

import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.entity.UserAccount;
import org.smj.utilitybilling.enums.Role;
import org.smj.utilitybilling.enums.UserStatus;
import org.smj.utilitybilling.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the default ROLE_ADMIN account on first application startup.
 */

@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userAccountRepository.existsByEmail("admin@utility.rw")) {
            UserAccount admin = UserAccount.builder()
                    .fullNames("System Administrator")
                    .email("admin@utility.rw")
                    .phoneNumber("250788000001")
                    .password(passwordEncoder.encode("Admin@123"))
                    .status(UserStatus.ACTIVE)
                    .role(Role.ROLE_ADMIN)
                    .build();
            userAccountRepository.save(admin);
        }
    }
}
