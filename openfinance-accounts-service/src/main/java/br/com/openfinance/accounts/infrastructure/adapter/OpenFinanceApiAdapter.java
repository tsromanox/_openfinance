package br.com.openfinance.accounts.infrastructure.adapter;

import br.com.openfinance.accounts.domain.entity.Account;
import br.com.openfinance.accounts.domain.entity.AccountBalance;
import br.com.openfinance.accounts.domain.entity.AccountLimit;
import br.com.openfinance.accounts.domain.port.OpenFinanceApiClient;
import br.com.openfinance.accounts.infrastructure.mapper.AccountMapper;
import br.com.openfinance.accounts.model.*;
import br.com.openfinance.core.client.BaseOpenFinanceClient;
import br.com.openfinance.core.security.OAuth2TokenProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class OpenFinanceApiAdapter extends BaseOpenFinanceClient implements OpenFinanceApiClient {

    private final AccountMapper mapper;

    public OpenFinanceApiAdapter(
            WebClient.Builder webClientBuilder,
            OAuth2TokenProvider tokenProvider,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            AccountMapper mapper) {

        // Base URL será configurada dinamicamente por instituição
        super(webClientBuilder, tokenProvider, circuitBreakerRegistry, retryRegistry, "", "accounts-api");
        this.mapper = mapper;
    }

    @Override
    public Mono<Account> getAccountDetails(String institutionId, String accountId, String consentId) {
        return executeRequest(
                tokenProvider.getToken()
                        .flatMap(token ->
                                webClient.get()
                                        .uri(getBaseUrl(institutionId) + "/accounts/{accountId}", accountId)
                                        .header("Authorization", "Bearer " + token)
                                        .header("x-fapi-interaction-id", java.util.UUID.randomUUID().toString())
                                        .header("consent-id", consentId)
                                        .retrieve()
                                        .bodyToMono(ResponseAccountIdentification.class)
                        )
                        .map(response -> mapper.toDomainAccount(response.getData()))
        );
    }

    @Override
    public Mono<AccountBalance> getAccountBalance(String institutionId, String accountId, String consentId) {
        return executeRequest(
                tokenProvider.getToken()
                        .flatMap(token ->
                                webClient.get()
                                        .uri(getBaseUrl(institutionId) + "/accounts/{accountId}/balances", accountId)
                                        .header("Authorization", "Bearer " + token)
                                        .header("x-fapi-interaction-id", java.util.UUID.randomUUID().toString())
                                        .header("consent-id", consentId)
                                        .retrieve()
                                        .bodyToMono(ResponseAccountBalances.class)
                        )
                        .map(response -> mapper.toDomainBalance(response.getData()))
        );
    }

    @Override
    public Mono<AccountLimit> getAccountLimits(String institutionId, String accountId, String consentId) {
        return executeRequest(
                tokenProvider.getToken()
                        .flatMap(token ->
                                webClient.get()
                                        .uri(getBaseUrl(institutionId) + "/accounts/{accountId}/overdraft-limits", accountId)
                                        .header("Authorization", "Bearer " + token)
                                        .header("x-fapi-interaction-id", java.util.UUID.randomUUID().toString())
                                        .header("consent-id", consentId)
                                        .retrieve()
                                        .bodyToMono(ResponseAccountOverdraftLimits.class)
                        )
                        .map(response -> mapper.toDomainLimit(response.getData()))
        );
    }

    private String getBaseUrl(String institutionId) {
        // Em produção, buscar URL do serviço de participantes
        return "https://api." + institutionId + ".com.br/open-banking/accounts/v2";
    }
}
