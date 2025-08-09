package br.com.openfinance.accounts.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private UUID accountId;
    private String externalAccountId;
    private String customerId;
    private String participantId;
    private String brandId;
    private String number;
    private String checkDigit;
    private AccountType type;
    private AccountSubType subtype;
    private String currency;
    private BigDecimal availableAmount;
    private BigDecimal blockedAmount;
    private BigDecimal automaticallyInvestedAmount;
    private BigDecimal overdraftContractedLimit;
    private BigDecimal overdraftUsedLimit;
    private BigDecimal unarrangedOverdraftAmount;
    private LocalDateTime lastUpdateDateTime;
    private LocalDateTime syncedAt;
    private AccountStatus status;

    @Builder.Default
    private List<AccountTransaction> transactions = new ArrayList<>();

    @Builder.Default
    private List<AccountBalance> balances = new ArrayList<>();

    public BigDecimal getTotalBalance() {
        return availableAmount
                .add(blockedAmount)
                .add(automaticallyInvestedAmount);
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public void updateBalance(BigDecimal available, BigDecimal blocked, BigDecimal invested) {
        this.availableAmount = available;
        this.blockedAmount = blocked;
        this.automaticallyInvestedAmount = invested;
        this.lastUpdateDateTime = LocalDateTime.now();
    }

    public void addTransaction(AccountTransaction transaction) {
        this.transactions.add(transaction);
        transaction.setAccount(this);
    }

    public void syncCompleted() {
        this.syncedAt = LocalDateTime.now();
    }
}
