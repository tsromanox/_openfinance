package br.com.openfinance.infrastructure.persistence.entity;

import br.com.openfinance.domain.consent.ConsentStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Optimized JPA entity for Consent with performance annotations and indexing.
 */
@Entity
@Table(name = "consents", indexes = {
    @Index(name = "idx_consents_org_status", columnList = "organizationId, status"),
    @Index(name = "idx_consents_created_at", columnList = "createdAt"),
    @Index(name = "idx_consents_expiration", columnList = "expirationDateTime"),
    @Index(name = "idx_consents_consent_id", columnList = "consentId", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@DynamicUpdate // Only update changed fields
@OptimisticLocking(type = OptimisticLockType.VERSION)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "consents")
public class ConsentEntity {
    
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;
    
    @Column(name = "consent_id", nullable = false, unique = true, length = 256)
    private String consentId;
    
    @Column(name = "organization_id", nullable = false, length = 100)
    private String organizationId;
    
    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;
    
    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ConsentStatus status;
    
    @Column(name = "permissions", nullable = false)
    @JdbcTypeCode(java.sql.Types.ARRAY)
    private String[] permissions;
    
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "expiration_date_time")
    private LocalDateTime expirationDateTime;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;
    
    // Lazy-loaded associated accounts for performance
    @OneToMany(mappedBy = "consent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 20) // Hibernate batch loading optimization
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<AccountEntity> accounts = new ArrayList<>();
    
    // Helper methods for domain conversion
    public Set<String> getPermissionsAsSet() {
        return permissions != null ? Set.of(permissions) : Set.of();
    }
    
    public void setPermissionsFromSet(Set<String> permissionSet) {
        this.permissions = permissionSet != null ? 
            permissionSet.toArray(new String[0]) : new String[0];
    }
    
    // Performance optimization: prevent N+1 queries
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}