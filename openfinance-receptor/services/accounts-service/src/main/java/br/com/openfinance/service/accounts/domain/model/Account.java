package br.com.openfinance.service.accounts.domain.model;

import br.com.openfinance.accounts.domain.valueobject.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade Account - Representa uma conta bancária no domínio
 */
public class Account {
    private final AccountId id;
    private final String accountId; // ID externo do Open Finance
    private final UUID consentId;
    private final String organizationId;
    private final String customerId;
    private final AccountIdentification identification;
    private final AccountType type;
    private final AccountSubType subtype;
    private final Currency currency;
    private final AccountStatus status;
    private final List<AccountBalance> balances;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastSyncAt;

    private Account(Builder builder) {
        this.id = builder.id;
        this.accountId = builder.accountId;
        this.consentId = builder.consentId;
        this.organizationId = builder.organizationId;
        this.customerId = builder.customerId;
        this.identification = builder.identification;
        this.type = builder.type;
        this.subtype = builder.subtype;
        this.currency = builder.currency;
        this.status = builder.status;
        this.balances = new ArrayList<>(builder.balances);
        this.createdAt = builder.createdAt;
        this.lastSyncAt = builder.lastSyncAt;
    }

    // Business methods
    public void updateBalance(AccountBalance newBalance) {
        this.balances.add(newBalance);
    }

    public boolean needsSync() {
        if (lastSyncAt == null) return true;
        return LocalDateTime.now().minusMinutes(15).isAfter(lastSyncAt);
    }

    public boolean isActive() {
        return status == AccountStatus.AVAILABLE;
    }

    public AccountBalance getCurrentBalance() {
        return balances.isEmpty() ? null :
                balances.get(balances.size() - 1);
    }

    // Getters
    public AccountId getId() { return id; }
    public String getAccountId() { return accountId; }
    public UUID getConsentId() { return consentId; }
    public String getOrganizationId() { return organizationId; }
    public String getCustomerId() { return customerId; }
    public AccountIdentification getIdentification() { return identification; }
    public AccountType getType() { return type; }
    public AccountSubType getSubtype() { return subtype; }
    public Currency getCurrency() { return currency; }
    public AccountStatus getStatus() { return status; }
    public List<AccountBalance> getBalances() { return new ArrayList<>(balances); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastSyncAt() { return lastSyncAt; }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AccountId id;
        private String accountId;
        private UUID consentId;
        private String organizationId;
        private String customerId;
        private AccountIdentification identification;
        private AccountType type;
        private AccountSubType subtype;
        private Currency currency = Currency.BRL;
        private AccountStatus status = AccountStatus.AVAILABLE;
        private List<AccountBalance> balances = new ArrayList<>();
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime lastSyncAt;

        public Builder id(AccountId id) {
            this.id = id;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder consentId(UUID consentId) {
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

        public Builder identification(AccountIdentification identification) {
            this.identification = identification;
            return this;
        }

        public Builder type(AccountType type) {
            this.type = type;
            return this;
        }

        public Builder subtype(AccountSubType subtype) {
            this.subtype = subtype;
            return this;
        }

        public Builder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public Builder status(AccountStatus status) {
            this.status = status;
            return this;
        }

        public Builder balances(List<AccountBalance> balances) {
            this.balances = balances;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastSyncAt(LocalDateTime lastSyncAt) {
            this.lastSyncAt = lastSyncAt;
            return this;
        }

        public Account build() {
            return new Account(this);
        }
    }
}
