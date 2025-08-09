package br.com.openfinance.accounts.application.services;

import br.com.openfinance.accounts.domain.model.Account;
import br.com.openfinance.accounts.domain.model.AccountTransaction;
import br.com.openfinance.accounts.domain.model.AccountBalance;
import br.com.openfinance.accounts.domain.ports.output.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class VirtualThreadAccountSyncService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;
    private final TransmitterAccountClient transmitterClient;

    public void syncCustomerAccountsWithVirtualThreads(String customerId, String participantId) {
        log.info("Syncing all accounts for customer {} using Virtual Threads", customerId);

        try {
            // Fetch all customer accounts from transmitter
            List<Account> accounts = transmitterClient.fetchAccountsByCustomer(participantId, customerId);

            // Use Virtual Threads to sync all accounts in parallel
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

                List<CompletableFuture<SyncResult>> futures = accounts.stream()
                        .map(account -> CompletableFuture.supplyAsync(
                                () -> syncSingleAccount(account, participantId),
                                executor))
                        .toList();

                // Wait for all syncs to complete
                CompletableFuture<Void> allOf = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]));

                allOf.get(5, TimeUnit.MINUTES);

                // Collect and log results
                long successCount = futures.stream()
                        .map(CompletableFuture::join)
                        .filter(SyncResult::success)
                        .count();

                log.info("Customer {} sync completed: {}/{} accounts synced successfully",
                        customerId, successCount, accounts.size());
            }

        } catch (Exception e) {
            log.error("Error syncing customer {} accounts", customerId, e);
            throw new RuntimeException("Failed to sync customer accounts", e);
        }
    }

    private SyncResult syncSingleAccount(Account account, String participantId) {
        try {
            // Save or update account
            account = accountRepository.save(account);

            // Sync transactions and balance in parallel using virtual threads
            CompletableFuture<Void> transactionsFuture = CompletableFuture.runAsync(
                    () -> syncAccountTransactions(account, participantId),
                    Thread.ofVirtual().factory());

            CompletableFuture<Void> balanceFuture = CompletableFuture.runAsync(
                    () -> syncAccountBalance(account, participantId),
                    Thread.ofVirtual().factory());

            // Wait for both to complete
            CompletableFuture.allOf(transactionsFuture, balanceFuture).join();

            return new SyncResult(account.getAccountId().toString(), true, null);

        } catch (Exception e) {
            log.error("Failed to sync account {}", account.getAccountId(), e);
            return new SyncResult(account.getAccountId().toString(), false, e.getMessage());
        }
    }

    private void syncAccountTransactions(Account account, String participantId) {
        LocalDate fromDate = LocalDate.now().minusDays(90);
        LocalDate toDate = LocalDate.now();

        List<AccountTransaction> transactions = transmitterClient.fetchTransactions(
                participantId,
                account.getExternalAccountId(),
                fromDate,
                toDate
        );

        // Process transactions in batches using virtual threads
        int batchSize = 100;
        List<List<AccountTransaction>> batches = partition(transactions, batchSize);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            batches.stream()
                    .map(batch -> CompletableFuture.runAsync(
                            () -> saveTransactionBatch(batch, account), executor))
                    .forEach(CompletableFuture::join);
        }

        log.debug("Synced {} transactions for account {}",
                transactions.size(), account.getAccountId());
    }

    private void syncAccountBalance(Account account, String participantId) {
        AccountBalance balance = transmitterClient.fetchBalance(
                participantId,
                account.getExternalAccountId()
        );

        balance.setAccount(account);
        balanceRepository.save(balance);

        log.debug("Synced balance for account {}", account.getAccountId());
    }

    private void saveTransactionBatch(List<AccountTransaction> batch, Account account) {
        batch.forEach(t -> t.setAccount(account));
        transactionRepository.saveAll(batch);
    }

    private <T> List<List<T>> partition(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    private record SyncResult(String accountId, boolean success, String error) {}
}
