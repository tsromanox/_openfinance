package br.com.openfinance.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Set;

public record ConsentResponse(
        @JsonProperty("consentId") String consentId,
        @JsonProperty("status") String status,
        @JsonProperty("statusUpdateDateTime") LocalDateTime statusUpdateDateTime,
        @JsonProperty("permissions") Set<String> permissions,
        @JsonProperty("expirationDateTime") LocalDateTime expirationDateTime,
        @JsonProperty("transactionFromDateTime") LocalDateTime transactionFromDateTime,
        @JsonProperty("transactionToDateTime") LocalDateTime transactionToDateTime
) {
    public String getStatus() {
        return status;
    }
}