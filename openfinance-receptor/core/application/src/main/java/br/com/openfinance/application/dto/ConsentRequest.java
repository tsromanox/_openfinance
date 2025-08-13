package br.com.openfinance.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Set;

public record ConsentRequest(
        @JsonProperty("permissions") Set<String> permissions,
        @JsonProperty("expirationDateTime") LocalDateTime expirationDateTime,
        @JsonProperty("transactionFromDateTime") LocalDateTime transactionFromDateTime,
        @JsonProperty("transactionToDateTime") LocalDateTime transactionToDateTime
) {}