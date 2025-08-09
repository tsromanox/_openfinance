package br.com.openfinance.consents.domain.exception;

public class ConsentNotFoundException extends RuntimeException {
    public ConsentNotFoundException(String message) {
        super(message);
    }
}
