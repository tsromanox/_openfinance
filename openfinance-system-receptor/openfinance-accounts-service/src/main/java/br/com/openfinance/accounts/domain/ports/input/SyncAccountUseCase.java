package br.com.openfinance.accounts.domain.ports.input;

import br.com.openfinance.accounts.domain.model.Account;
import java.util.UUID;

public interface SyncAccountUseCase {
    Account syncAccount(UUID accountId);
    void syncAccountsByCustomer(String customerId);
    void syncAllAccounts();
}
