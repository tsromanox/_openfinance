package br.com.openfinance.accounts.domain.entity;

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
public class AccountBalance {
    private BigDecimal availableAmount;
    private String availableAmountCurrency;
    private BigDecimal blockedAmount;
    private String blockedAmountCurrency;
    private BigDecimal automaticallyInvestedAmount;
    private String automaticallyInvestedAmountCurrency;
    private LocalDateTime updateDateTime;
}
