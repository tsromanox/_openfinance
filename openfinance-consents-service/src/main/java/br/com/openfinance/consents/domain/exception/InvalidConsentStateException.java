package br.com.openfinance.consents.domain.exception;

public class InvalidConsentStateException extends ConsentExtensionException {
    public InvalidConsentStateException(String code, String title, String detail) {
        super(code, title, detail);
    }
}
