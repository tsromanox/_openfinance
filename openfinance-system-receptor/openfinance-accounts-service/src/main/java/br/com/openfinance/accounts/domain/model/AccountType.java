package br.com.openfinance.accounts.domain.model;

public enum AccountType {
    CONTA_DEPOSITO_A_VISTA("CONTA_DEPOSITO_A_VISTA", "Conta Corrente"),
    CONTA_POUPANCA("CONTA_POUPANCA", "Conta Poupança"),
    CONTA_PAGAMENTO_PRE_PAGA("CONTA_PAGAMENTO_PRE_PAGA", "Conta Pré-paga");

    private final String value;
    private final String description;

    AccountType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
