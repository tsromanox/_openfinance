package br.com.openfinance.accounts.infrastructure.adapters.output.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountResponse {

    @JsonProperty("accountId")
    private String accountId;

    @JsonProperty("brandId")
    private String brandId;

    @JsonProperty("number")
    private String number;

    @JsonProperty("checkDigit")
    private String checkDigit;

    @JsonProperty("type")
    private String type;

    @JsonProperty("subtype")
    private String subtype;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("balances")
    private BalanceData balances;

    @Data
    public static class BalanceData {
        @JsonProperty("availableAmount")
        private BigDecimal availableAmount;

        @JsonProperty("blockedAmount")
        private BigDecimal blockedAmount;

        @JsonProperty("automaticallyInvestedAmount")
        private BigDecimal automaticallyInvestedAmount;
    }
}
