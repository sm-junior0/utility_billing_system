package org.smj.utilitybilling.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.entity.UserAccount;
import org.smj.utilitybilling.enums.UserStatus;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
/**
 * Service layer implementing CustomUserDetails business rules.
 */

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserAccount user = userAccountRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean enabled = user.getStatus() == UserStatus.ACTIVE;
        return new User(
                user.getEmail(),
                user.getPassword(),
                enabled,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}
