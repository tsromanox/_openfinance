package br.com.openfinance.consents.domain.exception;

public class ConsentExtensionFailedException extends RuntimeException {
    public ConsentExtensionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
