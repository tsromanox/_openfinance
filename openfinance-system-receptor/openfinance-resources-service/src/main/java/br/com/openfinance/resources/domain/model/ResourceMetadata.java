package br.com.openfinance.resources.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceMetadata {
    private String currency;
    private BigDecimal balance;
    private BigDecimal availableAmount;
    private BigDecimal blockedAmount;
    private LocalDate dueDate;
    private LocalDate expiryDate;
    private String contractNumber;
    private Map<String, Object> additionalInfo;

    public boolean hasBalance() {
        return balance != null && balance.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isExpired() {
        if (expiryDate == null) {
            return false;
        }
        return expiryDate.isBefore(LocalDate.now());
    }

    public BigDecimal getTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        if (availableAmount != null) {
            total = total.add(availableAmount);
        }
        if (blockedAmount != null) {
            total = total.add(blockedAmount);
        }
        return total;
    }
}