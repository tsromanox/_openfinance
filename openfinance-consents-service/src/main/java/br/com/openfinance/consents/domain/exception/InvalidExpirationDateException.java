package br.com.openfinance.consents.domain.exception;

public class InvalidExpirationDateException extends ConsentExtensionException {
    public InvalidExpirationDateException(String code, String title, String detail) {
        super(code, title, detail);
    }
}
