package br.com.openfinance.core.domain.valueobjects;

import jakarta.validation.constraints.NotBlank;
import java.util.Objects;

public record ConsentId(@NotBlank String value) {
    public ConsentId {
        Objects.requireNonNull(value, "ConsentId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ConsentId cannot be blank");
        }
    }

    public static ConsentId of(String value) {
        return new ConsentId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
