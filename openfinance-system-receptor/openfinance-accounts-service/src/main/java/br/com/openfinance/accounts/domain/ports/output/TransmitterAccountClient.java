package br.com.openfinance.accounts.domain.ports.output;

import br.com.openfinance.accounts.domain.model.Account;
import br.com.openfinance.accounts.domain.model.AccountTransaction;
import br.com.openfinance.accounts.domain.model.AccountBalance;
import java.time.LocalDate;
import java.util.List;

public interface TransmitterAccountClient {
    Account fetchAccount(String participantId, String accountId);
    List<Account> fetchAccountsByCustomer(String participantId, String customerId);
    List<AccountTransaction> fetchTransactions(
            String participantId,
            String accountId,
            LocalDate fromDate,
            LocalDate toDate
    );
    AccountBalance fetchBalance(String participantId, String accountId);
}
