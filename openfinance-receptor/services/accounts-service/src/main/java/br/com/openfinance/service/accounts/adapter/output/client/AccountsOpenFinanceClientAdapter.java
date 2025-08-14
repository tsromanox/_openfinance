package br.com.openfinance.service.accounts.adapter.output.client;

import br.com.openfinance.accounts.application.port.output.AccountsOpenFinanceClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class AccountsOpenFinanceClientAdapter implements AccountsOpenFinanceClient {

    private static final Logger log = LoggerFactory.getLogger(AccountsOpenFinanceClientAdapter.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    @Value("${openfinance.api.base-url:https://api.openfinance.com.br}")
    private String baseUrl;

    public AccountsOpenFinanceClientAdapter(
            RestClient.Builder restClientBuilder,
            CircuitBreaker accountsCircuitBreaker,
            Retry accountsRetry) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.circuitBreaker = accountsCircuitBreaker;
        this.retry = accountsRetry;
    }

    @Override
    public AccountListResponse getAccounts(String organizationId, String accessToken) {
        log.debug("Fetching accounts from organization: {}", organizationId);

        Supplier<AccountListResponse> supplier = () ->
                restClient.get()
                        .uri("/open-banking/accounts/v2/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                        .retrieve()
                        .body(AccountListResponse.class);

        return executeWithResilience(supplier, "getAccounts");
    }

    @Override
    public AccountDetailResponse getAccount(String organizationId, String accountId, String accessToken) {
        log.debug("Fetching account details: {}", accountId);

        Supplier<AccountDetailResponse> supplier = () ->
                restClient.get()
                        .uri("/open-banking/accounts/v2/accounts/{accountId}", accountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                        .retrieve()
                        .body(AccountDetailResponse.class);

        return executeWithResilience(supplier, "getAccount");
    }

    @Override
    public BalanceResponse getBalance(String organizationId, String accountId, String accessToken) {
        log.debug("Fetching balance for account: {}", accountId);

        Supplier<BalanceResponse> supplier = () ->
                restClient.get()
                        .uri("/open-banking/accounts/v2/accounts/{accountId}/balances", accountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                        .retrieve()
                        .body(BalanceResponse.class);

        return executeWithResilience(supplier, "getBalance");
    }

    @Override
    public TransactionsResponse getTransactions(
            String organizationId,
            String accountId,
            String accessToken,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page) {

        log.debug("Fetching transactions for account: {} from {} to {}",
                accountId, fromDate, toDate);

        Supplier<TransactionsResponse> supplier = () ->
                restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/open-banking/accounts/v2/accounts/{accountId}/transactions")
                                .queryParam("fromBookingDate", fromDate)
                                .queryParam("toBookingDate", toDate)
                                .queryParam("page", page)
                                .queryParam("page-size", 100)
                                .build(accountId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                        .retrieve()
                        .body(TransactionsResponse.class);

        return executeWithResilience(supplier, "getTransactions");
    }

    private <T> T executeWithResilience(Supplier<T> supplier, String operation) {
        Supplier<T> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, supplier);

        decoratedSupplier = Retry
                .decorateSupplier(retry, decoratedSupplier);

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            log.error("Error executing operation: {}", operation, e);
            throw new ExternalServiceException("Failed to execute: " + operation, e);
        }
    }

    public static class ExternalServiceException extends RuntimeException {
        public ExternalServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
