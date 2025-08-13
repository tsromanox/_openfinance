package br.com.openfinance.domain.exception;

/**
 * Exception thrown when a consent operation is invalid due to business rules.
 */
public class InvalidConsentException extends DomainException {
    
    public InvalidConsentException(String message) {
        super(message);
    }
    
    public InvalidConsentException(String message, Throwable cause) {
        super(message, cause);
    }
}