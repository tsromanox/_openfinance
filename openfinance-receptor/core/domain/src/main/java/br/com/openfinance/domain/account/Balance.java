package br.com.openfinance.domain.account;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Builder
@Getter
public class Balance {
    private final BigDecimal availableAmount;
    private final String availableAmountCurrency;
    private final BigDecimal blockedAmount;
    private final String blockedAmountCurrency;
    private final BigDecimal automaticallyInvestedAmount;
    private final String automaticallyInvestedAmountCurrency;
    private final LocalDateTime updatedAt;
    
    // Business logic methods
    public BigDecimal getTotalBalance() {
        BigDecimal available = availableAmount != null ? availableAmount : BigDecimal.ZERO;
        BigDecimal blocked = blockedAmount != null ? blockedAmount : BigDecimal.ZERO;
        BigDecimal invested = automaticallyInvestedAmount != null ? automaticallyInvestedAmount : BigDecimal.ZERO;
        
        return available.add(blocked).add(invested);
    }
    
    public boolean isPositive() {
        return getTotalBalance().compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean hasAvailableFunds() {
        return availableAmount != null && availableAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean hasBlockedFunds() {
        return blockedAmount != null && blockedAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean hasInvestedFunds() {
        return automaticallyInvestedAmount != null && automaticallyInvestedAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isRecent() {
        if (updatedAt == null) {
            return false;
        }
        // Considera recente se foi atualizado nas últimas 4 horas
        return updatedAt.isAfter(LocalDateTime.now().minusHours(4));
    }
    
    public boolean hasSameCurrency() {
        String baseCurrency = availableAmountCurrency;
        return Objects.equals(baseCurrency, blockedAmountCurrency) && 
               Objects.equals(baseCurrency, automaticallyInvestedAmountCurrency);
    }
    
    public String getPrimaryCurrency() {
        if (availableAmountCurrency != null) {
            return availableAmountCurrency;
        }
        if (blockedAmountCurrency != null) {
            return blockedAmountCurrency;
        }
        return automaticallyInvestedAmountCurrency;
    }
}
