package br.com.openfinance.application.port.input;

import br.com.openfinance.domain.account.Account;
import java.util.List;
import java.util.UUID;

public interface AccountUseCase {
    List<Account> syncAccountsForConsent(UUID consentId);
    Account getAccountDetails(String accountId);
    void updateAccountBalance(String accountId);
}
