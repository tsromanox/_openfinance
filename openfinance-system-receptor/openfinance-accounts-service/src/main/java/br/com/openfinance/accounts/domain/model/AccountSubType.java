package br.com.openfinance.accounts.domain.model;

public enum AccountSubType {
    INDIVIDUAL("INDIVIDUAL"),
    CONJUNTA_SIMPLES("CONJUNTA_SIMPLES"),
    CONJUNTA_SOLIDARIA("CONJUNTA_SOLIDARIA");

    private final String value;

    AccountSubType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
