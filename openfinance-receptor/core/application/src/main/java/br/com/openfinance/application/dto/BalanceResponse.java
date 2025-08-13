package br.com.openfinance.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceResponse(
        @JsonProperty("data") BalanceData data,
        @JsonProperty("links") Links links,
        @JsonProperty("meta") Meta meta
) {
    
    public record BalanceData(
            @JsonProperty("availableAmount") BigDecimal availableAmount,
            @JsonProperty("availableAmountCurrency") String availableAmountCurrency,
            @JsonProperty("blockedAmount") BigDecimal blockedAmount,
            @JsonProperty("blockedAmountCurrency") String blockedAmountCurrency,
            @JsonProperty("automaticallyInvestedAmount") BigDecimal automaticallyInvestedAmount,
            @JsonProperty("automaticallyInvestedAmountCurrency") String automaticallyInvestedAmountCurrency
    ) {}
    
    public record Links(
            @JsonProperty("self") String self
    ) {}
    
    public record Meta(
            @JsonProperty("requestDateTime") LocalDateTime requestDateTime
    ) {}
}