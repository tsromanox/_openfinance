package br.com.openfinance.consents.domain.exception;

public abstract class ConsentExtensionException extends RuntimeException {
    private final String code;
    private final String title;
    private final String detail;

    protected ConsentExtensionException(String code, String title, String detail) {
        super(detail);
        this.code = code;
        this.title = title;
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }
}
