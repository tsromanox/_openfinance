package br.com.openfinance.service.accounts.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade AccountBalance - Representa o saldo de uma conta
 */
public class AccountBalance {
    private final UUID id;
    private final AccountId accountId;
    private final BigDecimal availableAmount;
    private final BigDecimal blockedAmount;
    private final BigDecimal automaticallyInvestedAmount;
    private final BigDecimal overdraftContractedLimit;
    private final BigDecimal overdraftUsedLimit;
    private final Currency currency;
    private final LocalDateTime updateDateTime;

    private AccountBalance(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID();
        this.accountId = builder.accountId;
        this.availableAmount = builder.availableAmount;
        this.blockedAmount = builder.blockedAmount;
        this.automaticallyInvestedAmount = builder.automaticallyInvestedAmount;
        this.overdraftContractedLimit = builder.overdraftContractedLimit;
        this.overdraftUsedLimit = builder.overdraftUsedLimit;
        this.currency = builder.currency;
        this.updateDateTime = builder.updateDateTime;
    }

    // Business methods
    public BigDecimal getTotalAvailable() {
        return availableAmount.add(
                automaticallyInvestedAmount != null ? automaticallyInvestedAmount : BigDecimal.ZERO
        );
    }

    public BigDecimal getNetBalance() {
        return availableAmount.subtract(blockedAmount);
    }

    public boolean hasOverdraft() {
        return overdraftContractedLimit != null &&
                overdraftContractedLimit.compareTo(BigDecimal.ZERO) > 0;
    }

    // Getters
    public UUID getId() { return id; }
    public AccountId getAccountId() { return accountId; }
    public BigDecimal getAvailableAmount() { return availableAmount; }
    public BigDecimal getBlockedAmount() { return blockedAmount; }
    public BigDecimal getAutomaticallyInvestedAmount() { return automaticallyInvestedAmount; }
    public BigDecimal getOverdraftContractedLimit() { return overdraftContractedLimit; }
    public BigDecimal getOverdraftUsedLimit() { return overdraftUsedLimit; }
    public Currency getCurrency() { return currency; }
    public LocalDateTime getUpdateDateTime() { return updateDateTime; }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private AccountId accountId;
        private BigDecimal availableAmount = BigDecimal.ZERO;
        private BigDecimal blockedAmount = BigDecimal.ZERO;
        private BigDecimal automaticallyInvestedAmount = BigDecimal.ZERO;
        private BigDecimal overdraftContractedLimit;
        private BigDecimal overdraftUsedLimit;
        private Currency currency = Currency.BRL;
        private LocalDateTime updateDateTime = LocalDateTime.now();

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder accountId(AccountId accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder availableAmount(BigDecimal availableAmount) {
            this.availableAmount = availableAmount;
            return this;
        }

        public Builder blockedAmount(BigDecimal blockedAmount) {
            this.blockedAmount = blockedAmount;
            return this;
        }

        public Builder automaticallyInvestedAmount(BigDecimal amount) {
            this.automaticallyInvestedAmount = amount;
            return this;
        }

        public Builder overdraftContractedLimit(BigDecimal limit) {
            this.overdraftContractedLimit = limit;
            return this;
        }

        public Builder overdraftUsedLimit(BigDecimal used) {
            this.overdraftUsedLimit = used;
            return this;
        }

        public Builder currency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public Builder updateDateTime(LocalDateTime updateDateTime) {
            this.updateDateTime = updateDateTime;
            return this;
        }

        public AccountBalance build() {
            return new AccountBalance(this);
        }
    }
}
