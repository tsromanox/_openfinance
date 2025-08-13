package br.com.openfinance.resources.domain.model;

import lombok.Getter;

@Getter
public enum ResourceStatus {
    ACTIVE("ACTIVE", "Ativo"),
    INACTIVE("INACTIVE", "Inativo"),
    SUSPENDED("SUSPENDED", "Suspenso"),
    CANCELLED("CANCELLED", "Cancelado"),
    PENDING("PENDING", "Pendente");

    private final String code;
    private final String description;

    ResourceStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ResourceStatus fromCode(String code) {
        for (ResourceStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown resource status code: " + code);
    }
}