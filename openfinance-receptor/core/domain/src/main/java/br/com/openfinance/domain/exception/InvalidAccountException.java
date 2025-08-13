package br.com.openfinance.domain.exception;

/**
 * Exception thrown when an account operation is invalid due to business rules.
 */
public class InvalidAccountException extends DomainException {
    
    public InvalidAccountException(String message) {
        super(message);
    }
    
    public InvalidAccountException(String message, Throwable cause) {
        super(message, cause);
    }
}