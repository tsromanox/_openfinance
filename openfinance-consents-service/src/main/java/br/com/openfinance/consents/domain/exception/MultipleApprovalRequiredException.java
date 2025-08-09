package br.com.openfinance.consents.domain.exception;

public class MultipleApprovalRequiredException extends ConsentExtensionException {
    public MultipleApprovalRequiredException(String code, String title, String detail) {
        super(code, title, detail);
    }
}
