package br.com.openfinance.accounts.infrastructure.adapters.output.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BalanceResponse {

    @JsonProperty("availableAmount")
    private BigDecimal availableAmount;

    @JsonProperty("blockedAmount")
    private BigDecimal blockedAmount;

    @JsonProperty("automaticallyInvestedAmount")
    private BigDecimal automaticallyInvestedAmount;

    @JsonProperty("referenceDateTime")
    private LocalDateTime referenceDateTime;
}
