package org.smj.utilitybilling.service;

import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.entity.Customer;
import org.smj.utilitybilling.entity.UserAccount;
import org.smj.utilitybilling.exception.ForbiddenOperationException;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.CustomerRepository;
import org.smj.utilitybilling.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
/**
 * Service layer implementing CurrentUser business rules.
 */

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserAccountRepository userAccountRepository;
    private final CustomerRepository customerRepository;

    public UserAccount getUserByEmail(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Customer getCustomerByUserEmail(String email) {
        return customerRepository.findByOwnerUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No customer account is linked to this user"));
    }

    public void validateCustomerOwnership(Long customerId, String email) {
        Customer customer = getCustomerByUserEmail(email);
        if (!customer.getId().equals(customerId)) {
            throw new ForbiddenOperationException("You are not allowed to access another customer's records");
        }
    }
}
