package br.com.openfinance.resources.domain.model;

import lombok.Getter;

@Getter
public enum ResourceType {
    ACCOUNT("ACCOUNT", "Conta"),
    CREDIT_CARD("CREDIT_CARD", "Cartão de Crédito"),
    LOAN("LOAN", "Empréstimo"),
    FINANCING("FINANCING", "Financiamento"),
    INVESTMENT("INVESTMENT", "Investimento"),
    INSURANCE("INSURANCE", "Seguro"),
    PENSION("PENSION", "Previdência"),
    EXCHANGE("EXCHANGE", "Câmbio");

    private final String code;
    private final String description;

    ResourceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ResourceType fromCode(String code) {
        for (ResourceType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown resource type code: " + code);
    }
}