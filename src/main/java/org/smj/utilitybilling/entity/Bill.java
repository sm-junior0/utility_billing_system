package org.smj.utilitybilling.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.smj.utilitybilling.enums.BillStatus;

/**
 * Monthly utility bill for a customer meter, including charges, payments, and approval status.
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bills", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bill_reference", columnNames = "bill_reference"),
        @UniqueConstraint(name = "uk_bill_meter_period", columnNames = {"meter_id", "billing_month", "billing_year"}),
        @UniqueConstraint(name = "uk_bill_reading", columnNames = "reading_id")
})
public class Bill extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_reference", nullable = false, length = 50)
    private String billReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reading_id", nullable = false)
    private MeterReading reading;

    @Column(name = "billing_month", nullable = false)
    private Integer billingMonth;

    @Column(name = "billing_year", nullable = false)
    private Integer billingYear;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "consumption", nullable = false, precision = 14, scale = 2)
    private BigDecimal consumption;

    @Column(name = "tariff_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal tariffAmount;

    @Column(name = "service_charge_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal serviceChargeAmount;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "penalty_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "amount_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "outstanding_balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BillStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private UserAccount approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
