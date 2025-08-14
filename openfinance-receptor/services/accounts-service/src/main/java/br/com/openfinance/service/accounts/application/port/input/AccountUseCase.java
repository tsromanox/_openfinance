package br.com.openfinance.service.accounts.application.port.input;


import br.com.openfinance.service.accounts.domain.model.Account;
import br.com.openfinance.service.accounts.domain.model.AccountBalance;
import br.com.openfinance.service.accounts.domain.model.AccountTransaction;
import br.com.openfinance.service.accounts.domain.valueobject.AccountStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Porta de entrada - Casos de uso de Accounts
 */
public interface AccountUseCase {

    // Queries
    List<Account> getAccountsByConsent(UUID consentId);
    Account getAccountById(String accountId);
    AccountBalance getAccountBalance(String accountId);
    List<AccountTransaction> getAccountTransactions(
            String accountId,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer pageSize
    );

    // Commands
    void syncAccountsForConsent(UUID consentId);
    void syncAccountBalance(String accountId);
    void syncAccountTransactions(String accountId, Integer days);
    void updateAccountStatus(String accountId, AccountStatus status);

    // Batch operations
    void syncAllActiveAccounts();
    void processAccountsBatch(List<String> accountIds);
}
