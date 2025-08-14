package br.com.openfinance.service.accounts.adapter.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountDetailDto(
        @JsonProperty("compeCode") String compeCode,
        @JsonProperty("branchCode") String branchCode,
        @JsonProperty("number") String number,
        @JsonProperty("checkDigit") String checkDigit,
        @JsonProperty("type") String type,
        @JsonProperty("subtype") String subtype,
        @JsonProperty("currency") String currency
) {}
