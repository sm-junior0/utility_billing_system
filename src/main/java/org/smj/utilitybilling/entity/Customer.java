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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.smj.utilitybilling.enums.UserStatus;

/**
 * Registered utility customer profile linked to an optional login account.
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_national_id", columnNames = "national_id"),
        @UniqueConstraint(name = "uk_customer_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_customer_phone", columnNames = "phone_number"),
        @UniqueConstraint(name = "uk_customer_owner_user", columnNames = "owner_user_id")
})
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_names", nullable = false, length = 120)
    private String fullNames;

    @Column(name = "national_id", nullable = false, length = 16)
    private String nationalId;

    @Column(name = "email", nullable = false, length = 120)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private UserAccount ownerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private UserAccount approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
