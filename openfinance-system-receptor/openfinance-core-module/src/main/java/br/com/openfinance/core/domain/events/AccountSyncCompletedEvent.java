package br.com.openfinance.core.domain.events;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class AccountSyncCompletedEvent extends DomainEvent {
    private final String accountId;
    private final String customerId;
    private final String participantId;
    private final LocalDateTime syncedAt;
    private final int transactionCount;

    public AccountSyncCompletedEvent(String accountId, String customerId,
                                     String participantId, int transactionCount) {
        super(accountId);
        this.accountId = accountId;
        this.customerId = customerId;
        this.participantId = participantId;
        this.syncedAt = LocalDateTime.now();
        this.transactionCount = transactionCount;
    }
}
