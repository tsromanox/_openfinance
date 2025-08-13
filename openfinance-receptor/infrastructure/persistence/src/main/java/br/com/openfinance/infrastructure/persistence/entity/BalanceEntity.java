package br.com.openfinance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Optimized JPA entity for Balance with precision decimal handling.
 */
@Entity
@Table(name = "balances", indexes = {
    @Index(name = "idx_balances_account", columnList = "account_id"),
    @Index(name = "idx_balances_updated_at", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@DynamicUpdate
@OptimisticLocking(type = OptimisticLockType.VERSION)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "balances")
public class BalanceEntity {
    
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;
    
    @Column(name = "available_amount", precision = 19, scale = 4)
    private BigDecimal availableAmount;
    
    @Column(name = "available_amount_currency", length = 3)
    private String availableAmountCurrency;
    
    @Column(name = "blocked_amount", precision = 19, scale = 4)
    private BigDecimal blockedAmount;
    
    @Column(name = "blocked_amount_currency", length = 3)
    private String blockedAmountCurrency;
    
    @Column(name = "automatically_invested_amount", precision = 19, scale = 4)
    private BigDecimal automaticallyInvestedAmount;
    
    @Column(name = "automatically_invested_amount_currency", length = 3)
    private String automaticallyInvestedAmountCurrency;
    
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    // One-to-one relationship with account
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    @NotFound(action = NotFoundAction.IGNORE)
    private AccountEntity account;
    
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
    
    // Business logic methods
    public BigDecimal getTotalBalance() {
        BigDecimal available = availableAmount != null ? availableAmount : BigDecimal.ZERO;
        BigDecimal blocked = blockedAmount != null ? blockedAmount : BigDecimal.ZERO;
        BigDecimal invested = automaticallyInvestedAmount != null ? automaticallyInvestedAmount : BigDecimal.ZERO;
        
        return available.add(blocked).add(invested);
    }
    
    public boolean isRecent(int hoursThreshold) {
        if (updatedAt == null) {
            return false;
        }
        return updatedAt.isAfter(LocalDateTime.now().minusHours(hoursThreshold));
    }
}