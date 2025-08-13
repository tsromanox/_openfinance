package br.com.openfinance.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AccountsResponse(
        @JsonProperty("data") List<AccountData> data,
        @JsonProperty("links") Links links,
        @JsonProperty("meta") Meta meta
) {
    
    public record AccountData(
            @JsonProperty("accountId") String accountId,
            @JsonProperty("brandName") String brandName,
            @JsonProperty("companyCnpj") String companyCnpj,
            @JsonProperty("type") String type,
            @JsonProperty("subtype") String subtype,
            @JsonProperty("number") String number,
            @JsonProperty("checkDigit") String checkDigit,
            @JsonProperty("agencyNumber") String agencyNumber,
            @JsonProperty("agencyCheckDigit") String agencyCheckDigit,
            @JsonProperty("availableAmount") BigDecimal availableAmount,
            @JsonProperty("availableAmountCurrency") String availableAmountCurrency,
            @JsonProperty("blockedAmount") BigDecimal blockedAmount,
            @JsonProperty("blockedAmountCurrency") String blockedAmountCurrency,
            @JsonProperty("automaticallyInvestedAmount") BigDecimal automaticallyInvestedAmount,
            @JsonProperty("automaticallyInvestedAmountCurrency") String automaticallyInvestedAmountCurrency
    ) {}
    
    public record Links(
            @JsonProperty("self") String self,
            @JsonProperty("first") String first,
            @JsonProperty("prev") String prev,
            @JsonProperty("next") String next,
            @JsonProperty("last") String last
    ) {}
    
    public record Meta(
            @JsonProperty("totalRecords") Integer totalRecords,
            @JsonProperty("totalPages") Integer totalPages,
            @JsonProperty("requestDateTime") LocalDateTime requestDateTime
    ) {}
}