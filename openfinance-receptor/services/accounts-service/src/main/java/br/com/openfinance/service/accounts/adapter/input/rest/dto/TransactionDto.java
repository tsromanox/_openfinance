package br.com.openfinance.service.accounts.adapter.input.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record TransactionDto(
        @JsonProperty("transactionId") String transactionId,
        @JsonProperty("completedAuthorisedPaymentType") String completedAuthorisedPaymentType,
        @JsonProperty("creditDebitType") String creditDebitType,
        @JsonProperty("transactionName") String transactionName,
        @JsonProperty("type") String type,
        @JsonProperty("transactionAmount") MoneyDto transactionAmount,
        @JsonProperty("transactionDateTime") LocalDateTime transactionDateTime,
        @JsonProperty("partieCnpjCpf") String partieCnpjCpf
) {}
