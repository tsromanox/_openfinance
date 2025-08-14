package br.com.openfinance.service.accounts.adapter.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record BalanceDto(
        @JsonProperty("availableAmount") MoneyDto availableAmount,
        @JsonProperty("blockedAmount") MoneyDto blockedAmount,
        @JsonProperty("automaticallyInvestedAmount") MoneyDto automaticallyInvestedAmount,
        @JsonProperty("updateDateTime") LocalDateTime updateDateTime
) {}
