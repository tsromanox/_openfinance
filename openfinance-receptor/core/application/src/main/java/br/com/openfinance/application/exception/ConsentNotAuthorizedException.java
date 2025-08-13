package br.com.openfinance.application.exception;

import java.util.UUID;

public class ConsentNotAuthorizedException extends RuntimeException {
    
    public ConsentNotAuthorizedException(UUID consentId) {
        super("Consent is not authorized: " + consentId);
    }
}