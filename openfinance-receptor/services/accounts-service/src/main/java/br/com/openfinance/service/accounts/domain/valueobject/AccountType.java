package br.com.openfinance.service.accounts.domain.valueobject;

public enum AccountType {
    CONTA_DEPOSITO_A_VISTA("Conta Corrente"),
    CONTA_POUPANCA("Conta Poupança"),
    CONTA_PAGAMENTO_PRE_PAGA("Conta Pré-paga");

    private final String description;

    AccountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

