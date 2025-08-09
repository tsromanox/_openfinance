package br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity;

import br.com.openfinance.accounts.domain.model.AccountStatus;
import br.com.openfinance.accounts.domain.model.AccountSubType;
import br.com.openfinance.accounts.domain.model.AccountType;
import br.com.openfinance.core.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "accounts",
        indexes = {
                @Index(name = "idx_customer_id", columnList = "customer_id"),
                @Index(name = "idx_participant_id", columnList = "participant_id"),
                @Index(name = "idx_external_account", columnList = "external_account_id,participant_id"),
                @Index(name = "idx_last_update", columnList = "last_update_datetime")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_external_account",
                        columnNames = {"external_account_id", "participant_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "external_account_id", nullable = false)
    private String externalAccountId;

    @Column(name = "customer_id", nullable = false, length = 14)
    private String customerId;

    @Column(name = "participant_id", nullable = false)
    private String participantId;

    @Column(name = "brand_id", nullable = false)
    private String brandId;

    @Column(name = "account_number", nullable = false, length = 20)
    private String number;

    @Column(name = "check_digit", nullable = false, length = 2)
    private String checkDigit;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_subtype", nullable = false)
    private AccountSubType subtype;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "BRL";

    @Column(name = "available_amount", precision = 19, scale = 4)
    private BigDecimal availableAmount;

    @Column(name = "blocked_amount", precision = 19, scale = 4)
    private BigDecimal blockedAmount;

    @Column(name = "automatically_invested_amount", precision = 19, scale = 4)
    private BigDecimal automaticallyInvestedAmount;

    @Column(name = "overdraft_contracted_limit", precision = 19, scale = 4)
    private BigDecimal overdraftContractedLimit;

    @Column(name = "overdraft_used_limit", precision = 19, scale = 4)
    private BigDecimal overdraftUsedLimit;

    @Column(name = "unarranged_overdraft_amount", precision = 19, scale = 4)
    private BigDecimal unarrangedOverdraftAmount;

    @Column(name = "last_update_datetime", nullable = false)
    private LocalDateTime lastUpdateDateTime;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AccountTransactionEntity> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AccountBalanceEntity> balances = new ArrayList<>();

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AccountIdentificationEntity identification;

    @PrePersist
    @PreUpdate
    public void updateTimestamps() {
        if (lastUpdateDateTime == null) {
            lastUpdateDateTime = LocalDateTime.now();
        }
    }
}
