package br.com.openfinance.domain.consent;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Consent {
    private final UUID id;
    private final String consentId;
    private final String organizationId;
    private final String customerId;
    private final ConsentStatus status;
    private final Set<Permission> permissions;
    private final LocalDateTime createdAt;
    private final LocalDateTime expirationDateTime;

    private Consent(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "ID cannot be null");
        this.consentId = Objects.requireNonNull(builder.consentId, "ConsentId cannot be null");
        this.organizationId = Objects.requireNonNull(builder.organizationId, "OrganizationId cannot be null");
        this.customerId = Objects.requireNonNull(builder.customerId, "CustomerId cannot be null");
        this.status = Objects.requireNonNull(builder.status, "Status cannot be null");
        this.permissions = builder.permissions != null ? Set.copyOf(builder.permissions) : Set.of();
        this.createdAt = Objects.requireNonNull(builder.createdAt, "CreatedAt cannot be null");
        this.expirationDateTime = builder.expirationDateTime;
        
        validateBusinessRules();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public UUID getId() { return id; }
    public String getConsentId() { return consentId; }
    public String getOrganizationId() { return organizationId; }
    public String getCustomerId() { return customerId; }
    public ConsentStatus getStatus() { return status; }
    public Set<Permission> getPermissions() { return permissions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpirationDateTime() { return expirationDateTime; }

    // Business logic methods
    public boolean isExpired() {
        return expirationDateTime != null && LocalDateTime.now().isAfter(expirationDateTime);
    }

    public boolean isActive() {
        return status == ConsentStatus.AUTHORISED && !isExpired();
    }

    public boolean canAccessAccounts() {
        return isActive() && hasPermission(Permission.ACCOUNTS_READ);
    }

    public boolean canAccessBalances() {
        return isActive() && hasPermission(Permission.ACCOUNTS_BALANCES_READ);
    }

    public boolean canAccessResources() {
        return isActive() && hasPermission(Permission.RESOURCES_READ);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public Consent withStatus(ConsentStatus newStatus) {
        return builder()
                .id(this.id)
                .consentId(this.consentId)
                .organizationId(this.organizationId)
                .customerId(this.customerId)
                .status(newStatus)
                .permissions(this.permissions)
                .createdAt(this.createdAt)
                .expirationDateTime(this.expirationDateTime)
                .build();
    }

    private void validateBusinessRules() {
        if (organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("OrganizationId cannot be empty");
        }
        if (customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("CustomerId cannot be empty");
        }
        if (expirationDateTime != null && expirationDateTime.isBefore(createdAt)) {
            throw new IllegalArgumentException("Expiration date cannot be before creation date");
        }
    }

    public static class Builder {
        private UUID id;
        private String consentId;
        private String organizationId;
        private String customerId;
        private ConsentStatus status;
        private Set<Permission> permissions;
        private LocalDateTime createdAt;
        private LocalDateTime expirationDateTime;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder consentId(String consentId) {
            this.consentId = consentId;
            return this;
        }

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder status(ConsentStatus status) {
            this.status = status;
            return this;
        }

        public Builder permissions(Set<Permission> permissions) {
            this.permissions = permissions;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expirationDateTime(LocalDateTime expirationDateTime) {
            this.expirationDateTime = expirationDateTime;
            return this;
        }

        public Consent build() {
            return new Consent(this);
        }
    }
}
