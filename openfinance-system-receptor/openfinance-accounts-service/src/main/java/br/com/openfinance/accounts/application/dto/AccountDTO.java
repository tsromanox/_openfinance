package br.com.openfinance.accounts.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {
    private UUID accountId;
    private String brandId;
    private String number;
    private String checkDigit;
    private String type;
    private String subtype;
    private String currency;
    private BigDecimal availableAmount;
    private BigDecimal blockedAmount;
    private BigDecimal automaticallyInvestedAmount;
    private BigDecimal overdraftContractedLimit;
    private BigDecimal overdraftUsedLimit;
    private BigDecimal unarrangedOverdraftAmount;
    private LocalDateTime lastUpdateDateTime;
}
