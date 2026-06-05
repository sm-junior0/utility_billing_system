package org.smj.utilitybilling.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smj.utilitybilling.dto.request.PaymentRequest;
import org.smj.utilitybilling.dto.response.PaymentResponse;
import org.smj.utilitybilling.entity.Bill;
import org.smj.utilitybilling.entity.Payment;
import org.smj.utilitybilling.entity.UserAccount;
import org.smj.utilitybilling.enums.BillStatus;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.repository.PaymentRepository;
import org.smj.utilitybilling.util.ResponseMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing Payment business rules.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillService billService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public PaymentResponse recordPayment(PaymentRequest request, String financeEmail) {
        billService.applyLatePenalties();
        Bill bill = resolveBill(request);
        if (bill.getStatus() == BillStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Bill must be approved before payment can be recorded");
        }
        if (bill.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("This bill is already fully paid");
        }
        if (request.getAmountPaid().compareTo(bill.getOutstandingBalance()) > 0) {
            throw new BadRequestException("Amount paid cannot exceed the outstanding balance");
        }

        UserAccount financeUser = currentUserService.getUserByEmail(financeEmail);
        BigDecimal newAmountPaid = bill.getAmountPaid().add(request.getAmountPaid());
        BigDecimal newOutstanding = bill.getOutstandingBalance().subtract(request.getAmountPaid());

        bill.setAmountPaid(newAmountPaid);
        bill.setOutstandingBalance(newOutstanding);
        bill.setStatus(newOutstanding.compareTo(BigDecimal.ZERO) == 0 ? BillStatus.PAID : BillStatus.PARTIALLY_PAID);

        boolean fullPayment = newOutstanding.compareTo(BigDecimal.ZERO) == 0;

        Payment payment = Payment.builder()
                .bill(bill)
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(request.getPaymentDate())
                .recordedBy(financeUser)
                .approvedBy(financeUser)
                .approvedAt(request.getPaymentDate().atStartOfDay())
                .build();
        entityManager.flush();
        Payment savedPayment = paymentRepository.save(payment);
        entityManager.flush();
        if (fullPayment) {
            notificationService.sendPaymentCompletedNotification(bill);
            log.info("Bill fully paid billId={} reference={} totalPaid={}", bill.getId(), bill.getBillReference(), bill.getAmountPaid());
        } else {
            log.info("Partial payment recorded billId={} amount={} remaining={}", bill.getId(), request.getAmountPaid(), bill.getOutstandingBalance());
        }
        return ResponseMapper.toPaymentResponse(savedPayment);
    }

    public List<PaymentResponse> getPaymentsByBill(Long billId, String email, boolean customerAccess) {
        Bill bill = billService.getBillEntity(billId);
        if (customerAccess) {
            currentUserService.validateCustomerOwnership(bill.getCustomer().getId(), email);
        }
        return paymentRepository.findByBillId(billId).stream()
                .map(ResponseMapper::toPaymentResponse)
                .toList();
    }

    public List<PaymentResponse> getPaymentsByCustomer(Long customerId, String email, boolean customerAccess) {
        if (customerAccess) {
            currentUserService.validateCustomerOwnership(customerId, email);
            return paymentRepository.findByBillCustomerOwnerUserEmail(email).stream()
                    .map(ResponseMapper::toPaymentResponse)
                    .toList();
        }
        return paymentRepository.findByBillCustomerId(customerId).stream()
                .map(ResponseMapper::toPaymentResponse)
                .toList();
    }

    private Bill resolveBill(PaymentRequest request) {
        if (request.getBillReference() != null && !request.getBillReference().isBlank()) {
            return billService.getBillByReference(request.getBillReference().trim());
        }
        if (request.getBillId() != null) {
            return billService.getBillEntity(request.getBillId());
        }
        throw new BadRequestException("Either billId or billReference is required");
    }
}
