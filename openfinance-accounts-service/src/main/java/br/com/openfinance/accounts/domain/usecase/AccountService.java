package br.com.openfinance.accounts.domain.usecase;

import br.com.openfinance.accounts.domain.entity.Account;
import br.com.openfinance.accounts.domain.exception.AccountNotFoundException;
import br.com.openfinance.accounts.domain.port.AccountRepository;
import br.com.openfinance.accounts.domain.port.OpenFinanceApiClient;
import br.com.openfinance.core.metrics.OpenFinanceMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final OpenFinanceApiClient apiClient;
    private final OpenFinanceMetrics metrics;

    @Cacheable(value = "accounts", key = "#accountId")
    public Mono<Account> getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .doOnNext(account -> log.debug("Account {} retrieved from repository", accountId))
                .switchIfEmpty(Mono.error(new AccountNotFoundException(accountId)));
    }

    @Cacheable(value = "accounts-by-client", key = "#clientId")
    public Flux<Account> getAccountsByClient(String clientId) {
        return accountRepository.findByClientId(clientId)
                .doOnNext(account -> metrics.incrementProcessedAccounts("retrieved"));
    }

    @CacheEvict(value = {"accounts", "accounts-by-client"}, allEntries = true)
    public Mono<Account> updateAccountData(Account account) {
        String institutionId = account.getInstitutionId();
        String accountId = account.getAccountId();
        String consentId = account.getConsentId();

        long startTime = System.currentTimeMillis();

        return Mono.zip(
                        apiClient.getAccountDetails(institutionId, accountId, consentId),
                        apiClient.getAccountBalance(institutionId, accountId, consentId),
                        apiClient.getAccountLimits(institutionId, accountId, consentId)
                                .onErrorResume(error -> {
                                    log.warn("Failed to get limits for account {}: {}", accountId, error.getMessage());
                                    return Mono.empty();
                                })
                )
                .map(tuple -> {
                    Account updatedAccount = tuple.getT1();
                    updatedAccount.setBalance(tuple.getT2());
                    if (tuple.getT3() != null) {
                        updatedAccount.setLimit(tuple.getT3());
                    }
                    updatedAccount.setLastUpdated(LocalDateTime.now());
                    updatedAccount.setStatus("ACTIVE");
                    return updatedAccount;
                })
                .flatMap(accountRepository::save)
                .doOnSuccess(savedAccount -> {
                    long duration = System.currentTimeMillis() - startTime;
                    metrics.recordApiCall(institutionId, "account-update", 200, duration);
                    metrics.incrementProcessedAccounts("updated");
                    log.info("Account {} updated successfully in {}ms", accountId, duration);
                })
                .doOnError(error -> {
                    metrics.incrementErrors("account-update", error.getClass().getSimpleName());
                    log.error("Failed to update account {}", accountId, error);
                });
    }

    public Mono<Long> countAccountsByClient(String clientId) {
        return accountRepository.countByClientId(clientId);
    }
}
