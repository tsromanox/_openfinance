package br.com.openfinance.infrastructure.persistence.repository.reactive;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * R2DBC entity for reactive consent operations.
 * Optimized for non-blocking database access.
 */
@Table("consents")
public record ReactiveConsentEntity(
    @Id
    @Column("id")
    UUID id,
    
    @Column("consent_id")
    String consentId,
    
    @Column("organization_id")
    String organizationId,
    
    @Column("customer_id")
    String customerId,
    
    @Column("status")
    String status,
    
    @Column("permissions")
    String[] permissions,
    
    @Column("created_at")
    LocalDateTime createdAt,
    
    @Column("expiration_date_time")
    LocalDateTime expirationDateTime,
    
    @Column("updated_at")
    LocalDateTime updatedAt,
    
    @Version
    @Column("version")
    Long version
) {
    public Set<String> getPermissionsAsSet() {
        return permissions != null ? Set.of(permissions) : Set.of();
    }
    
    public ReactiveConsentEntity withPermissions(Set<String> permissionSet) {
        String[] permArray = permissionSet != null ? 
            permissionSet.toArray(new String[0]) : new String[0];
        
        return new ReactiveConsentEntity(
            id, consentId, organizationId, customerId, status,
            permArray, createdAt, expirationDateTime, updatedAt, version
        );
    }
    
    public ReactiveConsentEntity withStatus(String newStatus) {
        return new ReactiveConsentEntity(
            id, consentId, organizationId, customerId, newStatus,
            permissions, createdAt, expirationDateTime, LocalDateTime.now(), version
        );
    }
    
    public ReactiveConsentEntity withUpdatedAt(LocalDateTime newUpdatedAt) {
        return new ReactiveConsentEntity(
            id, consentId, organizationId, customerId, status,
            permissions, createdAt, expirationDateTime, newUpdatedAt, version
        );
    }
}