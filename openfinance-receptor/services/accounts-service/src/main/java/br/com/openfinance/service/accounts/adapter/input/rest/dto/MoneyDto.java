package br.com.openfinance.service.accounts.adapter.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MoneyDto(
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency
) {}
