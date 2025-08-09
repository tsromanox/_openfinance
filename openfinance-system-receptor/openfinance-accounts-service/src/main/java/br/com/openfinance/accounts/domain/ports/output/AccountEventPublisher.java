package br.com.openfinance.accounts.domain.ports.output;

import br.com.openfinance.accounts.domain.model.Account;

public interface AccountEventPublisher {
    void publishAccountSynced(Account account);
    void publishAccountUpdated(Account account);
    void publishBatchSyncCompleted(int totalAccounts);
}
