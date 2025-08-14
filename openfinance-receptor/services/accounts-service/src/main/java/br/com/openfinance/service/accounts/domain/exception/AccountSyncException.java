package br.com.openfinance.service.accounts.domain.exception;

public class AccountSyncException extends RuntimeException {
    public AccountSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
