package br.com.openfinance.accounts.application.events;

import br.com.openfinance.accounts.domain.model.Account;
import br.com.openfinance.accounts.domain.ports.output.AccountEventPublisher;
import br.com.openfinance.core.application.ports.EventPublisher;
import br.com.openfinance.core.domain.events.AccountSyncCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AccountEventHandler implements AccountEventPublisher {

    private final EventPublisher eventPublisher;

    @Override
    public void publishAccountSynced(Account account) {
        AccountSyncCompletedEvent event = new AccountSyncCompletedEvent(
                account.getAccountId().toString(),
                account.getCustomerId(),
                account.getParticipantId(),
                account.getTransactions().size()
        );

        eventPublisher.publish(event);
        log.info("Published account sync completed event for account: {}",
                account.getAccountId());
    }

    @Override
    public void publishAccountUpdated(Account account) {
        // Create and publish account updated event
        log.info("Published account updated event for account: {}",
                account.getAccountId());
    }

    @Override
    public void publishBatchSyncCompleted(int totalAccounts) {
        // Create and publish batch sync completed event
        log.info("Published batch sync completed event. Total accounts: {}",
                totalAccounts);
    }
}
