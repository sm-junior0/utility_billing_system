package org.smj.utilitybilling.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.smj.utilitybilling.dto.request.PrepaidTopUpRequest;
import org.smj.utilitybilling.dto.response.PrepaidTransactionResponse;
import org.smj.utilitybilling.entity.Customer;
import org.smj.utilitybilling.entity.Meter;
import org.smj.utilitybilling.entity.PrepaidTransaction;
import org.smj.utilitybilling.entity.Tariff;
import org.smj.utilitybilling.enums.MeterStatus;
import org.smj.utilitybilling.enums.MeterType;
import org.smj.utilitybilling.enums.TariffType;
import org.smj.utilitybilling.enums.UserStatus;
import org.smj.utilitybilling.exception.BadRequestException;
import org.smj.utilitybilling.repository.PrepaidTransactionRepository;
import org.smj.utilitybilling.util.ResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Service layer implementing PrepaidTransaction business rules.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrepaidTransactionService {

    private final PrepaidTransactionRepository prepaidTransactionRepository;
    private final MeterService meterService;
    private final CurrentUserService currentUserService;
    private final TariffService tariffService;

    @Transactional
    public PrepaidTransactionResponse topUp(PrepaidTopUpRequest request) {
        Meter meter = meterService.getMeterEntity(request.getMeterId());
        Customer customer = meter.getCustomer();

        if (meter.getStatus() != MeterStatus.ACTIVE) {
            throw new BadRequestException("Only active meters can receive prepaid top-ups");
        }
        if (customer.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Inactive customers cannot receive prepaid top-ups");
        }
        if (meter.getMeterType() != MeterType.ELECTRICITY) {
            throw new BadRequestException("Legacy prepaid support is available for electricity meters only");
        }

        BigDecimal unitsPurchased = calculateLegacyUnits(request.getAmount(), request.getTransactionDate());
        PrepaidTransaction transaction = PrepaidTransaction.builder()
                .meter(meter)
                .customer(customer)
                .amount(request.getAmount())
                .token(generateToken())
                .unitsPurchased(unitsPurchased)
                .paymentMethod(request.getPaymentMethod())
                .transactionDate(request.getTransactionDate())
                .build();
        return ResponseMapper.toPrepaidTransactionResponse(prepaidTransactionRepository.save(transaction));
    }

    public List<PrepaidTransactionResponse> getTransactionsByCustomer(Long customerId, String email, boolean customerAccess) {
        if (customerAccess) {
            currentUserService.validateCustomerOwnership(customerId, email);
            return prepaidTransactionRepository.findByCustomerOwnerUserEmail(email).stream()
                    .map(ResponseMapper::toPrepaidTransactionResponse)
                    .toList();
        }
        return prepaidTransactionRepository.findByCustomerId(customerId).stream()
                .map(ResponseMapper::toPrepaidTransactionResponse)
                .toList();
    }

    private BigDecimal calculateLegacyUnits(BigDecimal amount, LocalDate date) {
        return tariffService.findApplicableTariff(MeterType.ELECTRICITY, date)
                .filter(tariff -> tariff.getTariffType() == TariffType.FLAT)
                .map(Tariff::getFlatUnitPrice)
                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .map(price -> amount.divide(price, 2, RoundingMode.HALF_UP))
                .orElseGet(() -> amount.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
