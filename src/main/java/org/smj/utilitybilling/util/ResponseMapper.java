package org.smj.utilitybilling.util;

import java.util.List;
import org.smj.utilitybilling.dto.response.BillResponse;
import org.smj.utilitybilling.dto.response.CustomerResponse;
import org.smj.utilitybilling.dto.response.MeterReadingResponse;
import org.smj.utilitybilling.dto.response.MeterResponse;
import org.smj.utilitybilling.dto.response.NotificationResponse;
import org.smj.utilitybilling.dto.response.PaymentResponse;
import org.smj.utilitybilling.dto.response.PenaltyResponse;
import org.smj.utilitybilling.dto.response.PrepaidTransactionResponse;
import org.smj.utilitybilling.dto.response.ServiceChargeResponse;
import org.smj.utilitybilling.dto.response.TariffResponse;
import org.smj.utilitybilling.dto.response.TariffTierResponse;
import org.smj.utilitybilling.dto.response.TaxResponse;
import org.smj.utilitybilling.dto.response.UserResponse;
import org.smj.utilitybilling.entity.Bill;
import org.smj.utilitybilling.entity.Customer;
import org.smj.utilitybilling.entity.Meter;
import org.smj.utilitybilling.entity.MeterReading;
import org.smj.utilitybilling.entity.Notification;
import org.smj.utilitybilling.entity.Payment;
import org.smj.utilitybilling.entity.Penalty;
import org.smj.utilitybilling.entity.PrepaidTransaction;
import org.smj.utilitybilling.entity.ServiceCharge;
import org.smj.utilitybilling.entity.Tariff;
import org.smj.utilitybilling.entity.TariffTier;
import org.smj.utilitybilling.entity.Tax;
import org.smj.utilitybilling.entity.UserAccount;
/**
 * Utility helpers for ResponseMapper.
 */

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static UserResponse toUserResponse(UserAccount user) {
        Long customerId = user.getCustomer() != null ? user.getCustomer().getId() : null;
        return new UserResponse(user.getId(), user.getFullNames(), user.getEmail(), user.getPhoneNumber(), user.getStatus(),
                user.getRole(), customerId, user.getCreatedAt(), user.getUpdatedAt());
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        Long ownerUserId = customer.getOwnerUser() != null ? customer.getOwnerUser().getId() : null;
        return new CustomerResponse(customer.getId(), customer.getFullNames(), customer.getNationalId(), customer.getEmail(),
                customer.getPhoneNumber(), customer.getAddress(), customer.getStatus(), ownerUserId, customer.getCreatedAt(),
                customer.getUpdatedAt());
    }

    public static MeterResponse toMeterResponse(Meter meter) {
        return new MeterResponse(meter.getId(), meter.getMeterNumber(), meter.getMeterType(), meter.getInstallationDate(),
                meter.getStatus(), meter.getCustomer().getId(), meter.getCustomer().getFullNames(), meter.getCreatedAt(),
                meter.getUpdatedAt());
    }

    public static MeterReadingResponse toMeterReadingResponse(MeterReading reading) {
        return new MeterReadingResponse(reading.getId(), reading.getMeter().getId(), reading.getMeter().getMeterNumber(),
                reading.getPreviousReading(), reading.getCurrentReading(), reading.getConsumption(), reading.getReadingDate(),
                reading.getReadingMonth(), reading.getReadingYear(), reading.getCapturedBy().getFullNames(), reading.getCreatedAt(),
                reading.getUpdatedAt());
    }

    public static TariffResponse toTariffResponse(Tariff tariff, List<TariffTier> tiers) {
        List<TariffTierResponse> tierResponses = tiers.stream()
                .map(ResponseMapper::toTariffTierResponse)
                .toList();
        return new TariffResponse(tariff.getId(), tariff.getMeterType(), tariff.getTariffType(), tariff.getVersion(),
                tariff.getEffectiveFrom(), tariff.getEffectiveTo(), tariff.isActive(), tariff.getFlatUnitPrice(),
                tierResponses, tariff.getCreatedAt(), tariff.getUpdatedAt());
    }

    public static TariffTierResponse toTariffTierResponse(TariffTier tier) {
        return new TariffTierResponse(tier.getId(), tier.getMinUnits(), tier.getMaxUnits(), tier.getPricePerUnit());
    }

    public static TaxResponse toTaxResponse(Tax tax) {
        return new TaxResponse(tax.getId(), tax.getName(), tax.getPercentage(), tax.isActive(), tax.getCreatedAt(),
                tax.getUpdatedAt());
    }

    public static PenaltyResponse toPenaltyResponse(Penalty penalty) {
        return new PenaltyResponse(penalty.getId(), penalty.getName(), penalty.getPercentage(), penalty.isActive(),
                penalty.getCreatedAt(), penalty.getUpdatedAt());
    }

    public static ServiceChargeResponse toServiceChargeResponse(ServiceCharge serviceCharge) {
        return new ServiceChargeResponse(serviceCharge.getId(), serviceCharge.getMeterType(), serviceCharge.getAmount(),
                serviceCharge.isActive(), serviceCharge.getCreatedAt(), serviceCharge.getUpdatedAt());
    }

    public static BillResponse toBillResponse(Bill bill) {
        String approver = bill.getApprovedBy() != null ? bill.getApprovedBy().getFullNames() : null;
        return new BillResponse(bill.getId(), bill.getBillReference(), bill.getCustomer().getId(), bill.getCustomer().getFullNames(),
                bill.getMeter().getId(), bill.getMeter().getMeterNumber(), bill.getBillingMonth(), bill.getBillingYear(),
                bill.getDueDate(), bill.getConsumption(), bill.getTariffAmount(), bill.getServiceChargeAmount(),
                bill.getTaxAmount(), bill.getPenaltyAmount(), bill.getTotalAmount(), bill.getAmountPaid(),
                bill.getOutstandingBalance(), bill.getStatus(), approver, bill.getApprovedAt(), bill.getCreatedAt(),
                bill.getUpdatedAt());
    }

    public static PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getBill().getId(), payment.getBill().getBillReference(),
                payment.getAmountPaid(), payment.getPaymentMethod(), payment.getPaymentDate(), payment.getRecordedBy().getFullNames(),
                payment.getApprovedBy().getFullNames(), payment.getApprovedAt(), payment.getCreatedAt(), payment.getUpdatedAt());
    }

    public static PrepaidTransactionResponse toPrepaidTransactionResponse(PrepaidTransaction transaction) {
        return new PrepaidTransactionResponse(transaction.getId(), transaction.getMeter().getId(),
                transaction.getMeter().getMeterNumber(), transaction.getCustomer().getId(), transaction.getCustomer().getFullNames(),
                transaction.getAmount(), transaction.getToken(), transaction.getUnitsPurchased(), transaction.getPaymentMethod(),
                transaction.getTransactionDate(), transaction.getCreatedAt());
    }

    public static NotificationResponse toNotificationResponse(Notification notification) {
        Long billId = notification.getBill() != null ? notification.getBill().getId() : null;
        return new NotificationResponse(notification.getId(), notification.getCustomer().getId(),
                notification.getCustomer().getFullNames(), billId, notification.getMessage(), notification.getType(),
                notification.getStatus(), notification.getSentAt(), notification.getCreatedAt());
    }
}
