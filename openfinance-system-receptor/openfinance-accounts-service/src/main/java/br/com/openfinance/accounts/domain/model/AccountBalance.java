package br.com.openfinance.accounts.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalance {
    private UUID balanceId;
    private Account account;
    private BigDecimal availableAmount;
    private BigDecimal blockedAmount;
    private BigDecimal automaticallyInvestedAmount;
    private LocalDateTime referenceDateTime;

    public BigDecimal getTotalAmount() {
        return availableAmount
                .add(blockedAmount)
                .add(automaticallyInvestedAmount);
    }
}
