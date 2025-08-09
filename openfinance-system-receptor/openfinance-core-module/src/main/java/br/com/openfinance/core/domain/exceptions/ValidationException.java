package br.com.openfinance.core.domain.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

@Getter
public class ValidationException extends BusinessException {
    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.errors = new HashMap<>();
    }

    public ValidationException(String message, Map<String, String> errors) {
        super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.errors = errors;
    }

    public ValidationException(Map<String, String> errors) {
        super("Validation failed", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        this.errors = errors;
    }

    public void addError(String field, String message) {
        this.errors.put(field, message);
    }
}
