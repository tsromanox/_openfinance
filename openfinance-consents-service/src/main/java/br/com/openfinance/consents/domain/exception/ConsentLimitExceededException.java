package br.com.openfinance.consents.domain.exception;

public class ConsentLimitExceededException extends RuntimeException {
    public ConsentLimitExceededException(String message) {
        super(message);
    }
}
