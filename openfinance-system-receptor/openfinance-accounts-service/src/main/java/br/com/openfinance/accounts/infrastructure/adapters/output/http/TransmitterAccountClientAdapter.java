package br.com.openfinance.accounts.infrastructure.adapters.output.http;

import br.com.openfinance.accounts.domain.model.Account;
import br.com.openfinance.accounts.domain.model.AccountBalance;
import br.com.openfinance.accounts.domain.model.AccountTransaction;
import br.com.openfinance.accounts.domain.ports.output.TransmitterAccountClient;
import br.com.openfinance.accounts.infrastructure.adapters.output.http.dto.*;
import br.com.openfinance.core.application.ports.ParticipantClient;
import br.com.openfinance.core.infrastructure.http.RetryableHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransmitterAccountClientAdapter implements TransmitterAccountClient {

    private final RetryableHttpClient httpClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final ParticipantClient participantClient;
    private final TransmitterResponseMapper responseMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public Account fetchAccount(String participantId, String accountId) {
        log.info("Fetching account {} from participant {}", accountId, participantId);

        String url = buildUrl(participantId, "/accounts/v3/accounts/" + accountId);
        HttpHeaders headers = buildHeaders(participantId);

        ResponseEntity<AccountResponse> response = httpClient.get(url, headers, AccountResponse.class);

        return responseMapper.toDomain(response.getBody());
    }

    @Override
    public List<Account> fetchAccountsByCustomer(String participantId, String customerId) {
        log.info("Fetching accounts for customer {} from participant {}", customerId, participantId);

        String url = buildUrl(participantId, "/accounts/v3/accounts");
        HttpHeaders headers = buildHeaders(participantId);
        headers.set("x-customer-id", customerId);

        ResponseEntity<AccountListResponse> response = httpClient.get(url, headers, AccountListResponse.class);

        return response.getBody().getData().stream()
                .map(responseMapper::toDomain)
                .toList();
    }

    @Override
    public List<AccountTransaction> fetchTransactions(
            String participantId,
            String accountId,
            LocalDate fromDate,
            LocalDate toDate) {

        log.info("Fetching transactions for account {} from {} to {}",
                accountId, fromDate, toDate);

        String url = buildUrl(participantId,
                String.format("/accounts/v3/accounts/%s/transactions?fromDate=%s&toDate=%s",
                        accountId,
                        fromDate.format(DATE_FORMATTER),
                        toDate.format(DATE_FORMATTER)));

        HttpHeaders headers = buildHeaders(participantId);

        ResponseEntity<TransactionListResponse> response =
                httpClient.get(url, headers, TransactionListResponse.class);

        return response.getBody().getData().stream()
                .map(responseMapper::toTransactionDomain)
                .toList();
    }

    @Override
    public AccountBalance fetchBalance(String participantId, String accountId) {
        log.info("Fetching balance for account {} from participant {}", accountId, participantId);

        String url = buildUrl(participantId,
                String.format("/accounts/v3/accounts/%s/balances", accountId));

        HttpHeaders headers = buildHeaders(participantId);

        ResponseEntity<BalanceResponse> response = httpClient.get(url, headers, BalanceResponse.class);

        return responseMapper.toBalanceDomain(response.getBody());
    }

    private String buildUrl(String participantId, String path) {
        String baseUrl = participantClient.getParticipantUrl(participantId);
        return baseUrl + path;
    }

    private HttpHeaders buildHeaders(String participantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken(participantId));
        headers.set("x-fapi-interaction-id", UUID.randomUUID().toString());
        headers.set("x-fapi-auth-date", LocalDateTime.now().toString());
        headers.set("x-fapi-customer-ip-address", "10.1.1.1");
        headers.set("Accept", "application/json");
        return headers;
    }

    private String getAccessToken(String participantId) {
        // OAuth2 token retrieval logic
        return "access-token";
    }
}
