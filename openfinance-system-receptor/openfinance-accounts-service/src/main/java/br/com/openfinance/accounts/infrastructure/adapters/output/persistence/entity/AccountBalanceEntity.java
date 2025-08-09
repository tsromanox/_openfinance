package br.com.openfinance.accounts.infrastructure.adapters.output.persistence.entity;

import br.com.openfinance.core.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_balances",
        indexes = {
                @Index(name = "idx_balance_account", columnList = "account_id"),
                @Index(name = "idx_balance_reference", columnList = "reference_datetime")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "balance_id")
    private UUID balanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(name = "available_amount", precision = 19, scale = 4)
    private BigDecimal availableAmount;

    @Column(name = "blocked_amount", precision = 19, scale = 4)
    private BigDecimal blockedAmount;

    @Column(name = "automatically_invested_amount", precision = 19, scale = 4)
    private BigDecimal automaticallyInvestedAmount;

    @Column(name = "reference_datetime", nullable = false)
    private LocalDateTime referenceDateTime;
}
