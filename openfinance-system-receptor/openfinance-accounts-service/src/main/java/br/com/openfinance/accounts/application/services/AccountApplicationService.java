package br.com.openfinance.accounts.application.services;

import br.com.openfinance.accounts.domain.model.*;
import br.com.openfinance.accounts.domain.ports.input.*;
import br.com.openfinance.accounts.domain.ports.output.*;
import br.com.openfinance.accounts.domain.services.AccountDomainService;
import br.com.openfinance.core.domain.exceptions.BusinessException;
import br.com.openfinance.core.domain.exceptions.ResourceNotFoundException;
import br.com.openfinance.core.infrastructure.monitoring.Monitored;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AccountApplicationService implements
        GetAccountUseCase,
        ListAccountsUseCase,
        SyncAccountUseCase,
        GetAccountTransactionsUseCase,
        GetAccountBalanceUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;
    private final TransmitterAccountClient transmitterClient;
    private final AccountEventPublisher eventPublisher;
    private final AccountDomainService domainService;

    @Override
    @Monitored
    @Cacheable(value = "accounts", key = "#accountId")
    public Optional<Account> getAccount(UUID accountId) {
        log.debug("Getting account: {}", accountId);
        return accountRepository.findById(accountId);
    }

    @Override
    @Monitored
    public Optional<Account> getAccountByExternalId(String participantId, String externalAccountId) {
        log.debug("Getting account by external ID: {} from participant: {}",
                externalAccountId, participantId);
        return accountRepository.findByExternalId(participantId, externalAccountId);
    }

    @Override
    @Monitored
    public Page<Account> listAccountsByCustomer(String customerId, Pageable pageable) {
        log.debug("Listing accounts for customer: {}", customerId);
        return accountRepository.findByCustomerId(customerId, pageable);
    }

    @Override
    @Monitored
    public Page<Account> listAccountsByParticipant(String participantId, Pageable pageable) {
        log.debug("Listing accounts for participant: {}", participantId);
        return accountRepository.findByParticipantId(participantId, pageable);
    }

    @Override
    @Monitored
    @CacheEvict(value = "accounts", key = "#accountId")
    public Account syncAccount(UUID accountId) {
        log.info("Syncing account: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId.toString()));

        if (!domainService.shouldSyncAccount(account)) {
            log.info("Account {} was recently synced, skipping", accountId);
            return account;
        }

        try {
            // Fetch updated data from transmitter
            Account externalData = transmitterClient.fetchAccount(
                    account.getParticipantId(),
                    account.getExternalAccountId()
            );

            // Update account with external data
            domainService.updateAccountFromExternal(account, externalData);

            // Fetch and save transactions
            syncAccountTransactions(account);

            // Fetch and save current balance
            syncAccountBalance(account);

            // Save updated account
            account = accountRepository.save(account);

            // Publish event
            eventPublisher.publishAccountSynced(account);

            log.info("Successfully synced account: {}", accountId);
            return account;

        } catch (Exception e) {
            log.error("Error syncing account {}: {}", accountId, e.getMessage());
            throw new BusinessException("Failed to sync account", "SYNC_ERROR", e);
        }
    }

    @Override
    @Monitored
    public void syncAccountsByCustomer(String customerId) {
        log.info("Syncing all accounts for customer: {}", customerId);

        Page<Account> accounts = accountRepository.findByCustomerId(customerId, Pageable.unpaged());

        accounts.forEach(account -> {
            try {
                syncAccount(account.getAccountId());
            } catch (Exception e) {
                log.error("Error syncing account {} for customer {}: {}",
                        account.getAccountId(), customerId, e.getMessage());
            }
        });

        log.info("Completed syncing {} accounts for customer {}",
                accounts.getTotalElements(), customerId);
    }

    @Override
    @Monitored
    public void syncAllAccounts() {
        log.info("Starting sync for all accounts");

        List<Account> accounts = accountRepository.findAccountsForBatchUpdate(1000);
        int syncedCount = 0;
        int errorCount = 0;

        for (Account account : accounts) {
            try {
                syncAccount(account.getAccountId());
                syncedCount++;
            } catch (Exception e) {
                log.error("Error syncing account {}: {}",
                        account.getAccountId(), e.getMessage());
                errorCount++;
            }
        }

        eventPublisher.publishBatchSyncCompleted(syncedCount);
        log.info("Batch sync completed. Synced: {}, Errors: {}", syncedCount, errorCount);
    }

    @Override
    @Monitored
    public Page<AccountTransaction> getTransactions(
            UUID accountId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        log.debug("Getting transactions for account {} from {} to {}",
                accountId, fromDate, toDate);

        // Verify account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", accountId.toString()));

        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(23, 59, 59);

        return transactionRepository.findByAccountIdAndPeriod(
                accountId, fromDateTime, toDateTime, pageable);
    }

    @Override
    @Monitored
    @Cacheable(value = "balances", key = "#accountId")
    public Optional<AccountBalance> getCurrentBalance(UUID accountId) {
        log.debug("Getting current balance for account: {}", accountId);
        return balanceRepository.findLatestByAccountId(accountId);
    }

    private void syncAccountTransactions(Account account) {
        LocalDate fromDate = LocalDate.now().minusDays(90);
        LocalDate toDate = LocalDate.now();

        List<AccountTransaction> transactions = transmitterClient.fetchTransactions(
                account.getParticipantId(),
                account.getExternalAccountId(),
                fromDate,
                toDate
        );

        transactions.forEach(transaction -> {
            transaction.setAccount(account);
            domainService.validateTransaction(transaction);
        });

        transactionRepository.saveAll(transactions);
        log.info("Saved {} transactions for account {}",
                transactions.size(), account.getAccountId());
    }

    private void syncAccountBalance(Account account) {
        AccountBalance externalBalance = transmitterClient.fetchBalance(
                account.getParticipantId(),
                account.getExternalAccountId()
        );

        externalBalance.setAccount(account);
        balanceRepository.save(externalBalance);

        log.info("Saved balance for account {}", account.getAccountId());
    }
}
