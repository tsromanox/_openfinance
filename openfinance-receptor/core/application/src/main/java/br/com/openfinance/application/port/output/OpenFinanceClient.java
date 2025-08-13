package br.com.openfinance.application.port.output;

import br.com.openfinance.application.dto.AccountsResponse;
import br.com.openfinance.application.dto.BalanceResponse;
import br.com.openfinance.application.dto.ConsentRequest;
import br.com.openfinance.application.dto.ConsentResponse;

public interface OpenFinanceClient {
    ConsentResponse createConsent(String orgId, ConsentRequest request);
    ConsentResponse getConsent(String orgId, String consentId);
    AccountsResponse getAccounts(String orgId, String token);
    BalanceResponse getBalance(String orgId, String accountId, String token);
}
