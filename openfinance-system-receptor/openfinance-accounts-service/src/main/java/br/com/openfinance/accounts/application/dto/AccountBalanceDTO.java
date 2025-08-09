package br.com.openfinance.accounts.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceDTO {
    private BigDecimal availableAmount;
    private BigDecimal blockedAmount;
    private BigDecimal automaticallyInvestedAmount;
    private LocalDateTime referenceDateTime;
}
