package br.com.openfinance.service.accounts.domain.valueobject;

import java.util.UUID;

/**
 * Value Object - AccountId
 */
public class AccountId {
    private final UUID value;

    public AccountId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("AccountId cannot be null");
        }
        this.value = value;
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }

    public static AccountId of(UUID value) {
        return new AccountId(value);
    }

    public static AccountId of(String value) {
        return new AccountId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountId accountId = (AccountId) o;
        return value.equals(accountId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
