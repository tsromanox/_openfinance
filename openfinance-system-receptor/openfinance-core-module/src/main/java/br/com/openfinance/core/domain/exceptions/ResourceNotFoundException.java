package br.com.openfinance.core.domain.exceptions;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(
                String.format("%s not found with identifier: %s", resourceName, identifier),
                "RESOURCE_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
