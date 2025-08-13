package br.com.openfinance.domain.account;

/**
 * Enum representing different types of bank accounts according to Open Finance Brasil.
 */
public enum AccountType {
    CONTA_DEPOSITO_A_VISTA("CONTA_DEPOSITO_A_VISTA", "Conta de depósito à vista"),
    CONTA_POUPANCA("CONTA_POUPANCA", "Conta poupança"),
    CONTA_PAGAMENTO_PRE_PAGA("CONTA_PAGAMENTO_PRE_PAGA", "Conta de pagamento pré-paga");
    
    private final String code;
    private final String description;
    
    AccountType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static AccountType fromCode(String code) {
        for (AccountType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown account type code: " + code);
    }
    
    public boolean isCurrentAccount() {
        return this == CONTA_DEPOSITO_A_VISTA;
    }
    
    public boolean isSavingsAccount() {
        return this == CONTA_POUPANCA;
    }
    
    public boolean isPrepaidAccount() {
        return this == CONTA_PAGAMENTO_PRE_PAGA;
    }
}