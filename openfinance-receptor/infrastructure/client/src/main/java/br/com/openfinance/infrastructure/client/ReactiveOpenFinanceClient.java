package br.com.openfinance.infrastructure.client;

import br.com.openfinance.application.dto.AccountsResponse;
import br.com.openfinance.application.dto.BalanceResponse;
import br.com.openfinance.application.dto.ConsentRequest;
import br.com.openfinance.application.dto.ConsentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive interface for OpenFinance API operations optimized for parallel processing.
 */
public interface ReactiveOpenFinanceClient {
    
    /**
     * Creates a new consent.
     */
    Mono<ConsentResponse> createConsent(String orgId, ConsentRequest request);
    
    /**
     * Gets consent details by ID.
     */
    Mono<ConsentResponse> getConsent(String orgId, String consentId);
    
    /**
     * Gets accounts for a specific consent.
     */
    Mono<AccountsResponse> getAccounts(String orgId, String token);
    
    /**
     * Gets balance for a specific account.
     */
    Mono<BalanceResponse> getBalance(String orgId, String accountId, String token);
    
    /**
     * Gets balances for multiple accounts in parallel.
     */
    Flux<BalanceResponse> getBalances(String orgId, List<String> accountIds, String token);
    
    /**
     * Batch operation to get multiple consents.
     */
    Flux<ConsentResponse> getConsents(String orgId, List<String> consentIds);
    
    /**
     * Batch operation to get accounts for multiple organizations.
     */
    Flux<AccountsResponse> getAccountsForOrganizations(List<String> orgIds, String token);
}