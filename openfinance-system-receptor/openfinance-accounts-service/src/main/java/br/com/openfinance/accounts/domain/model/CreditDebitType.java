package br.com.openfinance.accounts.domain.model;

public enum CreditDebitType {
    CREDITO("CREDITO"),
    DEBITO("DEBITO");

    private final String value;

    CreditDebitType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
