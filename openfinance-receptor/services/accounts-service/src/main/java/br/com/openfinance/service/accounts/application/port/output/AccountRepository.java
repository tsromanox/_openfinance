package br.com.openfinance.service.accounts.application.port.output;


import br.com.openfinance.service.accounts.domain.model.Account;
import br.com.openfinance.service.accounts.domain.valueobject.AccountId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída - Repository de Accounts
 */
public interface AccountRepository {

    // Commands
    Account save(Account account);
    List<Account> saveAll(List<Account> accounts);
    void delete(AccountId accountId);

    // Queries
    Optional<Account> findById(AccountId accountId);
    Optional<Account> findByAccountId(String accountId);
    List<Account> findByConsentId(UUID consentId);
    List<Account> findByCustomerId(String customerId);
    List<Account> findByOrganizationId(String organizationId);
    List<Account> findAccountsNeedingSync(int limit);
    boolean existsByAccountId(String accountId);

    // Batch operations
    void updateLastSyncBatch(List<String> accountIds);
}
