package br.com.openfinance.application.exception;

import java.util.UUID;

public class ConsentNotFoundException extends RuntimeException {
    
    public ConsentNotFoundException(UUID consentId) {
        super("Consent not found: " + consentId);
    }
    
    public ConsentNotFoundException(String consentId) {
        super("Consent not found: " + consentId);
    }
}