package br.com.openfinance.service.accounts.application.service;


import br.com.openfinance.application.service.ConsentService;
import br.com.openfinance.domain.consent.Consent;
import br.com.openfinance.service.accounts.application.port.input.AccountUseCase;
import br.com.openfinance.service.accounts.application.port.output.AccountDataRepository;
import br.com.openfinance.service.accounts.application.port.output.AccountRepository;
import br.com.openfinance.service.accounts.application.port.output.AccountsOpenFinanceClient;
import br.com.openfinance.service.accounts.domain.exception.AccountSyncException;
import br.com.openfinance.service.accounts.domain.model.Account;
import br.com.openfinance.service.accounts.domain.model.AccountBalance;
import br.com.openfinance.service.accounts.domain.model.AccountTransaction;
import br.com.openfinance.service.accounts.domain.valueobject.*;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementação dos casos de uso de Accounts
 */
@Service
@Transactional
public class AccountService implements AccountUseCase {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final AccountDataRepository dataRepository;
    private final AccountsOpenFinanceClient openFinanceClient;
    private final ConsentService consentService;

    public AccountService(
            AccountRepository accountRepository,
            AccountDataRepository dataRepository,
            AccountsOpenFinanceClient openFinanceClient,
            ConsentService consentService) {
        this.accountRepository = accountRepository;
        this.dataRepository = dataRepository;
        this.openFinanceClient = openFinanceClient;
        this.consentService = consentService;
    }

    @Override
    @Cacheable(value = "accounts", key = "#consentId")
    public List<Account> getAccountsByConsent(UUID consentId) {
        log.debug("Getting accounts for consent: {}", consentId);
        return accountRepository.findByConsentId(consentId);
    }

    @Override
    @Cacheable(value = "account", key = "#accountId")
    public Account getAccountById(String accountId) {
        return accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Override
    @Timed(value = "accounts.balance.get")
    public AccountBalance getAccountBalance(String accountId) {
        var account = getAccountById(accountId);

        // Check if needs fresh data
        if (account.needsSync()) {
            syncAccountBalance(accountId);
            account = getAccountById(accountId);
        }

        return account.getCurrentBalance();
    }

    @Override
    public List<AccountTransaction> getAccountTransactions(
            String accountId,
            LocalDate fromDate,
            LocalDate toDate,
            Integer page,
            Integer pageSize) {

        log.debug("Getting transactions for account {} from {} to {}",
                accountId, fromDate, toDate);

        // Validate account exists
        getAccountById(accountId);

        return dataRepository.findTransactions(
                accountId, fromDate, toDate, page, pageSize
        );
    }

    @Override
    @Timed(value = "accounts.sync.consent")
    public void syncAccountsForConsent(UUID consentId) {
        log.info("Starting sync for consent: {}", consentId);

        try {
            // Get consent details
            var consent = consentService.getConsent(consentId);
            var accessToken = consentService.getAccessToken(consentId);

            // Fetch accounts from OpenFinance API
            var response = openFinanceClient.getAccounts(
                    consent.getOrganizationId(),
                    accessToken
            );

            // Convert and save accounts
            var accounts = response.accounts().stream()
                    .map(data -> mapToAccount(data, consentId, consent))
                    .collect(Collectors.toList());

            accountRepository.saveAll(accounts);

            // Sync balances for each account
            accounts.parallelStream().forEach(account ->
                    syncAccountBalance(account.getAccountId())
            );

            log.info("Successfully synced {} accounts for consent {}",
                    accounts.size(), consentId);

        } catch (Exception e) {
            log.error("Error syncing accounts for consent {}", consentId, e);
            throw new AccountSyncException("Failed to sync accounts", e);
        }
    }

    @Override
    @Timed(value = "accounts.sync.balance")
    public void syncAccountBalance(String accountId) {
        log.debug("Syncing balance for account: {}", accountId);

        try {
            var account = getAccountById(accountId);
            var consent = consentService.getConsent(account.getConsentId());
            var accessToken = consentService.getAccessToken(account.getConsentId());

            // Fetch balance from API
            var response = openFinanceClient.getBalance(
                    account.getOrganizationId(),
                    accountId,
                    accessToken
            );

            // Create and save new balance
            var balance = AccountBalance.builder()
                    .accountId(account.getId())
                    .availableAmount(response.balance().availableAmount())
                    .blockedAmount(response.balance().blockedAmount())
                    .automaticallyInvestedAmount(response.balance().automaticallyInvestedAmount())
                    .updateDateTime(response.balance().updateDateTime())
                    .build();

            dataRepository.saveBalance(balance);

            // Update account last sync time
            accountRepository.updateLastSyncBatch(List.of(accountId));

        } catch (Exception e) {
            log.error("Error syncing balance for account {}", accountId, e);
            throw new AccountSyncException("Failed to sync balance", e);
        }
    }

    @Override
    @Timed(value = "accounts.sync.transactions")
    public void syncAccountTransactions(String accountId, Integer days) {
        log.info("Syncing transactions for account {} for {} days", accountId, days);

        try {
            var account = getAccountById(accountId);
            var accessToken = consentService.getAccessToken(account.getConsentId());

            LocalDate fromDate = LocalDate.now().minusDays(days);
            LocalDate toDate = LocalDate.now();
            int page = 1;
            boolean hasMore = true;

            while (hasMore) {
                var response = openFinanceClient.getTransactions(
                        account.getOrganizationId(),
                        accountId,
                        accessToken,
                        fromDate,
                        toDate,
                        page
                );

                // Convert and save transactions
                var transactions = response.transactions().stream()
                        .filter(t -> !dataRepository.existsTransaction(t.transactionId()))
                        .map(data -> mapToTransaction(data, account.getId()))
                        .collect(Collectors.toList());

                if (!transactions.isEmpty()) {
                    dataRepository.saveTransactions(transactions);
                }

                hasMore = page < response.totalPages();
                page++;
            }

            log.info("Successfully synced transactions for account {}", accountId);

        } catch (Exception e) {
            log.error("Error syncing transactions for account {}", accountId, e);
            throw new AccountSyncException("Failed to sync transactions", e);
        }
    }

    @Override
    public void updateAccountStatus(String accountId, AccountStatus status) {
        log.debug("Updating account {} status to {}", accountId, status);

        var account = getAccountById(accountId);
        var updatedAccount = Account.builder()
                .id(account.getId())
                .accountId(account.getAccountId())
                .consentId(account.getConsentId())
                .organizationId(account.getOrganizationId())
                .customerId(account.getCustomerId())
                .identification(account.getIdentification())
                .type(account.getType())
                .subtype(account.getSubtype())
                .currency(account.getCurrency())
                .status(status) // Update status
                .balances(account.getBalances())
                .createdAt(account.getCreatedAt())
                .lastSyncAt(account.getLastSyncAt())
                .build();

        accountRepository.save(updatedAccount);
    }

    @Override
    @Async
    public void syncAllActiveAccounts() {
        log.info("Starting batch sync for all active accounts");

        var accountsToSync = accountRepository.findAccountsNeedingSync(1000);

        // Process in parallel using Virtual Threads
        var futures = accountsToSync.stream()
                .map(account -> CompletableFuture.runAsync(() -> {
                    try {
                        syncAccountBalance(account.getAccountId());
                    } catch (Exception e) {
                        log.error("Failed to sync account {}", account.getAccountId(), e);
                    }
                }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();

        log.info("Completed batch sync for {} accounts", accountsToSync.size());
    }

    @Override
    public void processAccountsBatch(List<String> accountIds) {
        log.info("Processing batch of {} accounts", accountIds.size());

        accountIds.parallelStream().forEach(accountId -> {
            try {
                syncAccountBalance(accountId);
                syncAccountTransactions(accountId, 7); // Last 7 days
            } catch (Exception e) {
                log.error("Error processing account {}", accountId, e);
            }
        });
    }

    // Mapping methods
    private Account mapToAccount(AccountsOpenFinanceClient.AccountData data, UUID consentId, Consent consent) {
        return Account.builder()
                .id(AccountId.generate())
                .accountId(data.accountId())
                .consentId(consentId)
                .organizationId(consent.getOrganizationId())
                .customerId(consent.getCustomerId())
                .identification(new AccountIdentification(
                        data.compeCode(),
                        data.branchCode(),
                        data.number(),
                        data.checkDigit()
                ))
                .type(AccountType.valueOf(data.type()))
                .subtype(AccountSubType.valueOf(data.subtype()))
                .currency(Currency.BRL)
                .status(AccountStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AccountTransaction mapToTransaction(AccountsOpenFinanceClient.TransactionData data, AccountId accountId) {
        return AccountTransaction.builder()
                .id(UUID.randomUUID())
                .transactionId(data.transactionId())
                .accountId(accountId)
                .type(TransactionType.valueOf(data.type()))
                .creditDebitType(CreditDebitType.valueOf(data.creditDebitType()))
                .transactionName(data.transactionName())
                .amount(data.amount())
                .currency(Currency.BRL)
                .transactionDateTime(data.transactionDateTime())
                .build();
    }
}
