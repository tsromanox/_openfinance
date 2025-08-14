package br.com.openfinance.service.accounts.adapter.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountDto(
        @JsonProperty("accountId") String accountId,
        @JsonProperty("brandName") String brandName,
        @JsonProperty("companyCnpj") String companyCnpj,
        @JsonProperty("type") String type,
        @JsonProperty("compeCode") String compeCode,
        @JsonProperty("branchCode") String branchCode,
        @JsonProperty("number") String number,
        @JsonProperty("checkDigit") String checkDigit
) {}
