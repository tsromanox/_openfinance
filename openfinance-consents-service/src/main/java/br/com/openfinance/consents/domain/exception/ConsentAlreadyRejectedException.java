package br.com.openfinance.consents.domain.exception;

public class ConsentAlreadyRejectedException extends RuntimeException {
    private final String code;

    public ConsentAlreadyRejectedException(String code) {
        super("Consent is already in rejected status");
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}