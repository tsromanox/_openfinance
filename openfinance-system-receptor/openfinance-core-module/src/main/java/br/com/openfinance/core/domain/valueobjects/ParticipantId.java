package br.com.openfinance.core.domain.valueobjects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;

public record ParticipantId(
        @NotBlank
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        String value
) {
    public ParticipantId {
        Objects.requireNonNull(value, "ParticipantId cannot be null");
        if (!value.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            throw new IllegalArgumentException("Invalid ParticipantId format");
        }
    }

    public static ParticipantId of(String value) {
        return new ParticipantId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
