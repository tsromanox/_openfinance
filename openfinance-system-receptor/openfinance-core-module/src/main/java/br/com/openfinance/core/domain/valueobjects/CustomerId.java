package br.com.openfinance.core.domain.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;

public record CustomerId(
        @NotBlank
        @Pattern(regexp = "^[0-9]{11}|[0-9]{14}$", message = "Must be a valid CPF or CNPJ")
        String value
) {
    public CustomerId {
        Objects.requireNonNull(value, "CustomerId cannot be null");
        if (!isValidCpfOrCnpj(value)) {
            throw new IllegalArgumentException("Invalid CPF/CNPJ");
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    private static boolean isValidCpfOrCnpj(String value) {
        String cleaned = value.replaceAll("[^0-9]", "");
        return cleaned.length() == 11 || cleaned.length() == 14;
    }

    public boolean isCpf() {
        return value.length() == 11;
    }

    public boolean isCnpj() {
        return value.length() == 14;
    }

    @Override
    public String toString() {
        return value;
    }
}
