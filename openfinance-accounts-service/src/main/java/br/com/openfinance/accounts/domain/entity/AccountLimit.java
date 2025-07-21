package br.com.openfinance.accounts.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLimit {
    private BigDecimal overdraftContractedLimit;
    private String overdraftContractedLimitCurrency;
    private BigDecimal overdraftUsedLimit;
    private String overdraftUsedLimitCurrency;
    private BigDecimal unarrangedOverdraftAmount;
    private String unarrangedOverdraftAmountCurrency;
}
