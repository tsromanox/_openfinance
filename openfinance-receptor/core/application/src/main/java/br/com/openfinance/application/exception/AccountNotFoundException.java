package br.com.openfinance.application.exception;

public class AccountNotFoundException extends RuntimeException {
    
    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }
}