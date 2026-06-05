package org.smj.utilitybilling.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smj.utilitybilling.dto.response.PageResponse;
import org.smj.utilitybilling.dto.response.UserResponse;
import org.smj.utilitybilling.entity.UserAccount;
import org.smj.utilitybilling.enums.Role;
import org.smj.utilitybilling.enums.UserStatus;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.UserAccountRepository;
import org.smj.utilitybilling.util.PageableUtils;
import org.smj.utilitybilling.util.ResponseMapper;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing User business rules.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final Set<String> ALLOWED_USER_SORT_FIELDS = Set.of(
            "id", "fullNames", "email", "phoneNumber", "status", "role", "createdAt", "updatedAt");

    private final UserAccountRepository userAccountRepository;

    public PageResponse<UserResponse> getUsers(Pageable pageable) {
        Pageable safePageable = PageableUtils.withDefaultSort(
                PageableUtils.validateSort(pageable, ALLOWED_USER_SORT_FIELDS, "id"), "id");
        Page<UserResponse> page = userAccountRepository.findAll(safePageable)
                .map(ResponseMapper::toUserResponse);
        log.info("Listed users page={} size={} totalElements={}", page.getNumber(), page.getSize(), page.getTotalElements());
        return PageResponse.from(page);
    }

    public UserResponse getUser(Long id) {
        log.debug("Fetching user id={}", id);
        return ResponseMapper.toUserResponse(getUserEntity(id));
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatus status) {
        UserAccount user = getUserEntity(id);
        log.info("Updating user id={} status from {} to {}", id, user.getStatus(), status);
        user.setStatus(status);
        return ResponseMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateRole(Long id, Role role) {
        UserAccount user = getUserEntity(id);
        log.info("Updating user id={} role from {} to {}", id, user.getRole(), role);
        user.setRole(role);
        return ResponseMapper.toUserResponse(user);
    }

    public UserAccount getUserEntity(Long id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
