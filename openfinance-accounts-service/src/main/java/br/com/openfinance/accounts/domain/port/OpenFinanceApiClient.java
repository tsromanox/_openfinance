package br.com.openfinance.accounts.domain.port;

import br.com.openfinance.accounts.domain.entity.Account;
import br.com.openfinance.accounts.domain.entity.AccountBalance;
import br.com.openfinance.accounts.domain.entity.AccountLimit;
import reactor.core.publisher.Mono;

public interface OpenFinanceApiClient {
    Mono<Account> getAccountDetails(String institutionId, String accountId, String consentId);
    Mono<AccountBalance> getAccountBalance(String institutionId, String accountId, String consentId);
    Mono<AccountLimit> getAccountLimits(String institutionId, String accountId, String consentId);
}
