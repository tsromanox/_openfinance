package br.com.openfinance.service.accounts.application.port.output;


import br.com.openfinance.service.accounts.domain.model.AccountBalance;
import br.com.openfinance.service.accounts.domain.model.AccountTransaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Porta de saída - Repository de Balances e Transactions
 */
public interface AccountDataRepository {

    // Balance operations
    AccountBalance saveBalance(AccountBalance balance);
    Optional<AccountBalance> findLatestBalance(String accountId);
    List<AccountBalance> findBalanceHistory(String accountId, int days);

    // Transaction operations
    List<AccountTransaction> saveTransactions(List<AccountTransaction> transactions);
    List<AccountTransaction> findTransactions(
            String accountId,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer pageSize
    );
    boolean existsTransaction(String transactionId);
    void deleteOldTransactions(int daysToKeep);
}
