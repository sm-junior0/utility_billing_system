package org.smj.utilitybilling.service;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smj.utilitybilling.dto.request.CustomerProfileRequest;
import org.smj.utilitybilling.dto.request.CustomerRequest;
import org.smj.utilitybilling.dto.response.CustomerResponse;
import org.smj.utilitybilling.dto.response.PageResponse;
import org.smj.utilitybilling.entity.Customer;
import org.smj.utilitybilling.entity.UserAccount;
import org.smj.utilitybilling.enums.Role;
import org.smj.utilitybilling.enums.UserStatus;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.CustomerRepository;
import org.smj.utilitybilling.repository.UserAccountRepository;
import org.smj.utilitybilling.util.PageableUtils;
import org.smj.utilitybilling.util.ResponseMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing Customer business rules.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private static final Set<String> ALLOWED_CUSTOMER_SORT_FIELDS = Set.of(
            "id", "fullNames", "nationalId", "email", "phoneNumber", "address", "status", "createdAt", "updatedAt");

    private final CustomerRepository customerRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        validateUniqueFields(request, null);
        Customer customer = buildCustomer(new Customer(), request);
        return ResponseMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse submitCustomerProfile(CustomerProfileRequest request, String email) {
        UserAccount ownerUser = currentUserService.getUserByEmail(email);
        if (ownerUser.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE users can submit a customer profile");
        }
        if (customerRepository.findByOwnerUserId(ownerUser.getId()).isPresent()) {
            throw new BadRequestException("A customer profile is already linked to this user account");
        }
        customerRepository.findAll().forEach(existing -> {
            if (existing.getNationalId().equals(request.getNationalId())) {
                throw new BadRequestException("National ID is already registered");
            }
            if (existing.getEmail().equalsIgnoreCase(ownerUser.getEmail())) {
                throw new BadRequestException("Customer email is already registered");
            }
            if (existing.getPhoneNumber().equals(ownerUser.getPhoneNumber())) {
                throw new BadRequestException("Customer phone number is already registered");
            }
        });

        Customer customer = Customer.builder()
                .fullNames(ownerUser.getFullNames())
                .nationalId(request.getNationalId())
                .email(ownerUser.getEmail())
                .phoneNumber(ownerUser.getPhoneNumber())
                .address(request.getAddress())
                .status(UserStatus.PENDING_APPROVAL)
                .ownerUser(ownerUser)
                .approvedBy(null)
                .approvedAt(null)
                .build();
        return ResponseMapper.toCustomerResponse(customerRepository.save(customer));
    }

    public PageResponse<CustomerResponse> getAllCustomers(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        Page<CustomerResponse> page = customerRepository.findAll(safePageable)
                .map(ResponseMapper::toCustomerResponse);
        log.info("Listed customers page={} size={} totalElements={}", page.getNumber(), page.getSize(), page.getTotalElements());
        return PageResponse.from(page);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        return PageableUtils.withDefaultSort(
                PageableUtils.validateSort(pageable, ALLOWED_CUSTOMER_SORT_FIELDS, "id"), "id");
    }

    public CustomerResponse getCustomer(Long id, String email, boolean customerAccess) {
        if (customerAccess) {
            Customer customer = customerRepository.findByOwnerUserEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            if (!customer.getId().equals(id)) {
                throw new BadRequestException("You can only view your own customer record");
            }
            return ResponseMapper.toCustomerResponse(customer);
        }
        return ResponseMapper.toCustomerResponse(getCustomerEntity(id));
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        validateUniqueFields(request, id);
        Customer customer = getCustomerEntity(id);
        return ResponseMapper.toCustomerResponse(customerRepository.save(buildCustomer(customer, request)));
    }

    @Transactional
    public CustomerResponse updateStatus(Long id, UserStatus status, String actorEmail) {
        Customer customer = getCustomerEntity(id);
        if (status == UserStatus.ACTIVE && customer.getStatus() == UserStatus.PENDING_APPROVAL) {
            return approveCustomer(customer, actorEmail);
        }
        customer.setStatus(status);
        return ResponseMapper.toCustomerResponse(customer);
    }

    @Transactional
    public CustomerResponse approveCustomer(Long id, String actorEmail) {
        Customer customer = getCustomerEntity(id);
        return approveCustomer(customer, actorEmail);
    }

    public Customer getCustomerEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer with ID " + id + " was not found. "
                                + "Create the customer first with POST /api/customers, then use the returned customer id for meters."
                ));
    }

    private Customer buildCustomer(Customer customer, CustomerRequest request) {
        customer.setFullNames(request.getFullNames());
        customer.setNationalId(request.getNationalId());
        customer.setEmail(request.getEmail());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setAddress(request.getAddress());
        customer.setStatus(request.getStatus() == null ? UserStatus.ACTIVE : request.getStatus());
        if (customer.getStatus() != UserStatus.ACTIVE) {
            customer.setApprovedBy(null);
            customer.setApprovedAt(null);
        }

        if (request.getOwnerUserId() != null) {
            customer.setOwnerUser(resolveOwnerUser(request.getOwnerUserId(), customer.getId()));
        } else {
            customer.setOwnerUser(null);
        }
        return customer;
    }

    private UserAccount resolveOwnerUser(Long ownerUserId, Long currentCustomerId) {
        UserAccount ownerUser = userAccountRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Owner user with ID " + ownerUserId + " was not found. "
                                + "Use GET /api/users after signup to get the real userId from the signup response or user list."
                ));
        Customer existingLinkedCustomer = customerRepository.findByOwnerUserId(ownerUserId).orElse(null);
        boolean sameLinkedCustomer = existingLinkedCustomer != null
                && currentCustomerId != null
                && existingLinkedCustomer.getId().equals(currentCustomerId);
        if (!sameLinkedCustomer && ownerUser.getRole() != Role.ROLE_CUSTOMER) {
            throw new BadRequestException(
                    "Only users with ROLE_CUSTOMER can be linked to a customer. "
                            + "User " + ownerUserId + " (" + ownerUser.getEmail() + ") has role " + ownerUser.getRole() + "."
            );
        }
        if (ownerUser.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException(
                    "Owner user must be ACTIVE before linking. "
                            + "User " + ownerUserId + " (" + ownerUser.getEmail() + ") has status " + ownerUser.getStatus()
                            + ". Verify OTP first, then retry customer creation."
            );
        }
        customerRepository.findByOwnerUserId(ownerUserId).ifPresent(existing -> {
            if (currentCustomerId == null || !existing.getId().equals(currentCustomerId)) {
                throw new BadRequestException(
                        "User " + ownerUserId + " is already linked to customer ID " + existing.getId() + "."
                );
            }
        });
        return ownerUser;
    }

    private void validateUniqueFields(CustomerRequest request, Long customerId) {
        customerRepository.findAll().forEach(existing -> {
            boolean sameId = customerId != null && existing.getId().equals(customerId);
            if (!sameId && existing.getNationalId().equals(request.getNationalId())) {
                throw new BadRequestException("National ID is already registered");
            }
            if (!sameId && existing.getEmail().equalsIgnoreCase(request.getEmail())) {
                throw new BadRequestException("Customer email is already registered");
            }
            if (!sameId && existing.getPhoneNumber().equals(request.getPhoneNumber())) {
                throw new BadRequestException("Customer phone number is already registered");
            }
        });
    }

    private CustomerResponse approveCustomer(Customer customer, String actorEmail) {
        if (customer.getStatus() != UserStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only customer profiles in PENDING_APPROVAL can be approved");
        }
        UserAccount approver = currentUserService.getUserByEmail(actorEmail);
        customer.setStatus(UserStatus.ACTIVE);
        customer.setApprovedBy(approver);
        customer.setApprovedAt(LocalDateTime.now());
        notificationService.sendCustomerActivationNotification(customer);
        return ResponseMapper.toCustomerResponse(customer);
    }
}
