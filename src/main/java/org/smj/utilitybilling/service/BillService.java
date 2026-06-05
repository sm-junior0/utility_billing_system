package org.smj.utilitybilling.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smj.utilitybilling.dto.request.BillGenerationRequest;
import org.smj.utilitybilling.dto.response.BillResponse;
import org.smj.utilitybilling.dto.response.PageResponse;
import org.smj.utilitybilling.entity.Bill;
import org.smj.utilitybilling.entity.Customer;
import org.smj.utilitybilling.entity.Meter;
import org.smj.utilitybilling.entity.MeterReading;
import org.smj.utilitybilling.entity.Penalty;
import org.smj.utilitybilling.entity.ServiceCharge;
import org.smj.utilitybilling.entity.Tariff;
import org.smj.utilitybilling.entity.TariffTier;
import org.smj.utilitybilling.entity.Tax;
import org.smj.utilitybilling.entity.UserAccount;
import org.smj.utilitybilling.enums.BillStatus;
import org.smj.utilitybilling.enums.MeterStatus;
import org.smj.utilitybilling.enums.TariffType;
import org.smj.utilitybilling.enums.UserStatus;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.exception.ResourceNotFoundException;
import org.smj.utilitybilling.repository.BillRepository;
import org.smj.utilitybilling.util.PageableUtils;
import org.smj.utilitybilling.util.ResponseMapper;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates and approves monthly utility bills using tariff, tax, penalty, and service charge rules.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillService {

    private static final Set<String> ALLOWED_BILL_SORT_FIELDS = Set.of(
            "id", "billReference", "billingMonth", "billingYear", "dueDate", "consumption",
            "totalAmount", "amountPaid", "outstandingBalance", "status", "approvedAt", "createdAt", "updatedAt");

    private final BillRepository billRepository;
    private final MeterReadingService meterReadingService;
    private final TariffService tariffService;
    private final TaxService taxService;
    private final PenaltyService penaltyService;
    private final ServiceChargeService serviceChargeService;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.default-bill-due-days}")
    private int dueDays;

    @Transactional
    public BillResponse generateBill(BillGenerationRequest request) {
        MeterReading reading = meterReadingService.getReadingEntity(request.getReadingId());
        Meter meter = reading.getMeter();
        Customer customer = meter.getCustomer();

        if (customer.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Inactive customers cannot receive bills");
        }
        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BadRequestException("Inactive meters cannot receive bills");
        }
        if (billRepository.existsByMeterIdAndBillingMonthAndBillingYear(
                meter.getId(), reading.getReadingMonth(), reading.getReadingYear())) {
            throw new BadRequestException("A bill already exists for this meter and billing period");
        }

        Tariff tariff = tariffService.getApplicableTariff(meter.getMeterType(), reading.getReadingDate());
        ServiceCharge serviceCharge = serviceChargeService.getActiveCharge(meter.getMeterType());
        Tax tax = taxService.getActiveTax();
        Penalty penalty = penaltyService.getActivePenaltyOrNull();

        BigDecimal tariffAmount = calculateTariffAmount(reading.getConsumption(), tariff);
        BigDecimal serviceChargeAmount = serviceCharge.getAmount();
        BigDecimal taxAmount = percentageOf(tariffAmount.add(serviceChargeAmount), tax.getPercentage());
        BigDecimal penaltyAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = tariffAmount.add(serviceChargeAmount).add(taxAmount).add(penaltyAmount);

        Bill bill = Bill.builder()
                .billReference(generateBillReference(reading))
                .customer(customer)
                .meter(meter)
                .reading(reading)
                .billingMonth(reading.getReadingMonth())
                .billingYear(reading.getReadingYear())
                .dueDate(LocalDate.now().plusDays(dueDays))
                .consumption(reading.getConsumption())
                .tariffAmount(tariffAmount)
                .serviceChargeAmount(serviceChargeAmount)
                .taxAmount(taxAmount)
                .penaltyAmount(penaltyAmount)
                .totalAmount(totalAmount)
                .amountPaid(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .outstandingBalance(totalAmount)
                .status(BillStatus.PENDING_APPROVAL)
                .approvedBy(null)
                .approvedAt(null)
                .build();

        Bill savedBill = billRepository.save(bill);
        entityManager.flush();
        notificationService.sendBillGeneratedNotification(savedBill);
        log.info("Bill generated id={} reference={} customerId={} total={}", savedBill.getId(),
                savedBill.getBillReference(), customer.getId(), savedBill.getTotalAmount());
        return ResponseMapper.toBillResponse(savedBill);
    }

    @Transactional
    public BillResponse approveBill(Long billId, String approverEmail) {
        Bill bill = getBillEntity(billId);
        if (bill.getStatus() != BillStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only pending bills can be approved");
        }
        UserAccount approver = currentUserService.getUserByEmail(approverEmail);
        bill.setStatus(BillStatus.APPROVED);
        bill.setApprovedBy(approver);
        bill.setApprovedAt(LocalDateTime.now());
        return ResponseMapper.toBillResponse(bill);
    }

    public PageResponse<BillResponse> getAllBills(Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        Page<BillResponse> page = billRepository.findAll(safePageable)
                .map(ResponseMapper::toBillResponse);
        log.info("Listed bills page={} size={} totalElements={}", page.getNumber(), page.getSize(), page.getTotalElements());
        return PageResponse.from(page);
    }

    public PageResponse<BillResponse> getBillsByCustomer(Long customerId, String email, boolean customerAccess, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        if (customerAccess) {
            currentUserService.validateCustomerOwnership(customerId, email);
            Page<BillResponse> page = billRepository.findByCustomerOwnerUserEmail(email, safePageable)
                    .map(ResponseMapper::toBillResponse);
            log.info("Listed bills for customer email={} page={} size={} totalElements={}",
                    email, page.getNumber(), page.getSize(), page.getTotalElements());
            return PageResponse.from(page);
        }
        Page<BillResponse> page = billRepository.findByCustomerId(customerId, safePageable)
                .map(ResponseMapper::toBillResponse);
        log.info("Listed bills for customerId={} page={} size={} totalElements={}",
                customerId, page.getNumber(), page.getSize(), page.getTotalElements());
        return PageResponse.from(page);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        return PageableUtils.withDefaultSort(
                PageableUtils.validateSort(pageable, ALLOWED_BILL_SORT_FIELDS, "id"), "id");
    }

    public BillResponse getBill(Long billId, String email, boolean customerAccess) {
        Bill bill = getBillEntity(billId);
        if (customerAccess) {
            currentUserService.validateCustomerOwnership(bill.getCustomer().getId(), email);
        }
        return ResponseMapper.toBillResponse(bill);
    }

    public Bill getBillEntity(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
    }

    public Bill getBillByReference(String billReference) {
        return billRepository.findByBillReference(billReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bill not found for reference: " + billReference
                ));
    }

    @Transactional
    public void applyLatePenalties() {
        entityManager.createNativeQuery("CALL cursor_apply_late_penalties()").executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private BigDecimal calculateTariffAmount(BigDecimal consumption, Tariff tariff) {
        if (tariff.getTariffType() == TariffType.FLAT) {
            return consumption.multiply(tariff.getFlatUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        }

        List<TariffTier> tiers = tariffService.getTariffTiers(tariff.getId());
        BigDecimal total = BigDecimal.ZERO;
        for (TariffTier tier : tiers) {
            if (consumption.compareTo(tier.getMinUnits()) <= 0) {
                continue;
            }

            BigDecimal upperLimit = tier.getMaxUnits() == null ? consumption : tier.getMaxUnits().min(consumption);
            BigDecimal unitsInTier = upperLimit.subtract(tier.getMinUnits());
            if (unitsInTier.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(unitsInTier.multiply(tier.getPricePerUnit()));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentageOf(BigDecimal amount, BigDecimal percentage) {
        return amount.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String generateBillReference(MeterReading reading) {
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BILL-" + reading.getReadingYear() + String.format("%02d", reading.getReadingMonth()) + "-" + randomPart;
    }
}
