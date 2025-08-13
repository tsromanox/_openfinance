package br.com.openfinance.application.exception;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ResourceNotFoundException(String resourceId, String resourceType) {
        super(String.format("Resource of type '%s' with ID '%s' not found", resourceType, resourceId));
    }
}