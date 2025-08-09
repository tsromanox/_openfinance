package br.com.openfinance.accounts.application.services;

import br.com.openfinance.accounts.domain.model.Account;
import br.com.openfinance.accounts.domain.ports.output.AccountEventPublisher;
import br.com.openfinance.accounts.domain.ports.output.AccountRepository;
import br.com.openfinance.accounts.domain.ports.output.TransmitterAccountClient;
import br.com.openfinance.accounts.domain.services.AccountDomainService;
import br.com.openfinance.core.infrastructure.monitoring.MetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountBatchProcessor {

    private final AccountRepository accountRepository;
    private final TransmitterAccountClient transmitterClient;
    private final AccountEventPublisher eventPublisher;
    private final AccountDomainService domainService;
    private final MetricsCollector metricsCollector;

    @Value("${openfinance.accounts.batch.size:1000}")
    private int batchSize;

    @Value("${openfinance.accounts.batch.parallel-threads:20}")
    private int parallelThreads;

    private final ExecutorService executorService = Executors.newFixedThreadPool(20);

    @Scheduled(cron = "${openfinance.accounts.batch.schedule.morning}")
    public void morningBatchUpdate() {
        log.info("Starting morning batch account update");
        processAccountUpdates();
    }

    @Scheduled(cron = "${openfinance.accounts.batch.schedule.evening}")
    public void eveningBatchUpdate() {
        log.info("Starting evening batch account update");
        processAccountUpdates();
    }

    public void processAccountUpdates() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("Starting batch account update process at {}", startTime);

        long totalAccounts = accountRepository.count();
        int totalPages = (int) Math.ceil((double) totalAccounts / batchSize);

        log.info("Total accounts: {}, Total batches: {}", totalAccounts, totalPages);

        List<CompletableFuture<BatchResult>> futures = IntStream.range(0, totalPages)
                .mapToObj(pageNumber -> CompletableFuture.supplyAsync(
                        () -> processBatch(pageNumber), executorService))
                .toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        try {
            allFutures.get(2, TimeUnit.HOURS);

            List<BatchResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            int totalSuccess = results.stream()
                    .mapToInt(BatchResult::successCount)
                    .sum();
            int totalErrors = results.stream()
                    .mapToInt(BatchResult::errorCount)
                    .sum();

            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(startTime, endTime).toMinutes();

            log.info("Batch update completed in {} minutes. Success: {}, Errors: {}",
                    duration, totalSuccess, totalErrors);

            eventPublisher.publishBatchSyncCompleted(totalSuccess);

        } catch (TimeoutException e) {
            log.error("Batch processing timeout after 2 hours", e);
        } catch (Exception e) {
            log.error("Error in batch processing", e);
        }
    }

    private BatchResult processBatch(int batchNumber) {
        log.debug("Processing batch {}", batchNumber);

        PageRequest pageRequest = PageRequest.of(batchNumber, batchSize);
        Page<Account> accounts = accountRepository.findAll(pageRequest);

        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        for (Account account : accounts) {
            try {
                updateAccount(account);
                successCount++;
                metricsCollector.recordAccountSynced();
            } catch (Exception e) {
                errorCount++;
                errors.add(String.format("Account %s: %s",
                        account.getAccountId(), e.getMessage()));
                log.error("Error updating account {}: {}",
                        account.getAccountId(), e.getMessage());
            }
        }

        log.info("Batch {} completed: {} success, {} errors",
                batchNumber, successCount, errorCount);

        return new BatchResult(batchNumber, successCount, errorCount, errors);
    }

    private void updateAccount(Account account) {
        if (!domainService.shouldSyncAccount(account)) {
            log.debug("Account {} recently synced, skipping", account.getAccountId());
            return;
        }

        // Fetch updated account data from transmitter
        Account externalData = transmitterClient.fetchAccount(
                account.getParticipantId(),
                account.getExternalAccountId()
        );

        // Update account with new data
        domainService.updateAccountFromExternal(account, externalData);

        // Save updated account
        accountRepository.save(account);

        log.debug("Account {} updated successfully", account.getAccountId());
    }

    private record BatchResult(
            int batchNumber,
            int successCount,
            int errorCount,
            List<String> errors
    ) {}
}
