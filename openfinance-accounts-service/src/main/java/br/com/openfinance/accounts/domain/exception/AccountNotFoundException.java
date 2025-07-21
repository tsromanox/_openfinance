package br.com.openfinance.accounts.domain.exception;

import lombok.Getter;

@Getter
public class AccountNotFoundException extends RuntimeException {

    private final String accountId;
    private final String errorCode;

    public AccountNotFoundException(String accountId) {
        super(String.format("Account not found: %s", accountId));
        this.accountId = accountId;
        this.errorCode = "ACCOUNT_NOT_FOUND";
    }

    public AccountNotFoundException(String accountId, String message) {
        super(message);
        this.accountId = accountId;
        this.errorCode = "ACCOUNT_NOT_FOUND";
    }

    public AccountNotFoundException(String accountId, String message, Throwable cause) {
        super(message, cause);
        this.accountId = accountId;
        this.errorCode = "ACCOUNT_NOT_FOUND";
    }
}
