package br.com.openfinance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Optimized JPA entity for Account with performance annotations and relationship management.
 */
@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_accounts_consent_org", columnList = "consent_id, organization_id"),
    @Index(name = "idx_accounts_account_id", columnList = "account_id", unique = true),
    @Index(name = "idx_accounts_last_sync", columnList = "last_sync_at"),
    @Index(name = "idx_accounts_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@DynamicUpdate
@OptimisticLocking(type = OptimisticLockType.VERSION)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "accounts")
public class AccountEntity {
    
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;
    
    @Column(name = "account_id", nullable = false, unique = true, length = 100)
    private String accountId;
    
    @Column(name = "brand_name", length = 100)
    private String brandName;
    
    @Column(name = "company_cnpj", length = 14)
    private String companyCnpj;
    
    @Column(name = "type", nullable = false, length = 30)
    private String type;
    
    @Column(name = "subtype", length = 30)
    private String subtype;
    
    @Column(name = "number", nullable = false, length = 20)
    private String number;
    
    @Column(name = "check_digit", length = 2)
    private String checkDigit;
    
    @Column(name = "agency_number", nullable = false, length = 10)
    private String agencyNumber;
    
    @Column(name = "agency_check_digit", length = 2)
    private String agencyCheckDigit;
    
    @Column(name = "organization_id", nullable = false, length = 100)
    private String organizationId;
    
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;
    
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    // Optimized relationship with consent
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consent_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE) // Handle soft deletes gracefully
    private ConsentEntity consent;
    
    // One-to-one relationship with balance for performance
    @OneToOne(mappedBy = "account", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private BalanceEntity balance;
    
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
    
    // Helper method for sync status
    public boolean needsSync(int hoursThreshold) {
        if (lastSyncAt == null) {
            return true;
        }
        return lastSyncAt.isBefore(LocalDateTime.now().minusHours(hoursThreshold));
    }
}