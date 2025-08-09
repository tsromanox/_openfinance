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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class VirtualThreadAccountBatchProcessor {

    private final AccountRepository accountRepository;
    private final TransmitterAccountClient transmitterClient;
    private final AccountEventPublisher eventPublisher;
    private final AccountDomainService domainService;
    private final MetricsCollector metricsCollector;

    @Value("${openfinance.accounts.batch.size:1000}")
    private int batchSize;

    @Value("${openfinance.accounts.batch.virtual-threads.max:10000}")
    private int maxVirtualThreads;

    @Value("${openfinance.accounts.batch.timeout-minutes:120}")
    private int timeoutMinutes;

    // Virtual Thread Executor
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Semaphore to limit concurrent virtual threads
    private Semaphore virtualThreadSemaphore;

    @Scheduled(cron = "${openfinance.accounts.batch.schedule.morning}")
    public void morningBatchUpdate() {
        log.info("Starting morning batch account update with Virtual Threads");
        processAccountUpdatesWithVirtualThreads();
    }

    @Scheduled(cron = "${openfinance.accounts.batch.schedule.evening}")
    public void eveningBatchUpdate() {
        log.info("Starting evening batch account update with Virtual Threads");
        processAccountUpdatesWithVirtualThreads();
    }

    public void processAccountUpdatesWithVirtualThreads() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("Starting Virtual Thread batch processing at {}", startTime);

        // Initialize semaphore to control virtual thread creation
        virtualThreadSemaphore = new Semaphore(maxVirtualThreads);

        long totalAccounts = accountRepository.count();

        log.info("Processing {} accounts using Virtual Threads", totalAccounts);

        // Metrics
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalProcessingTime = new AtomicLong(0);

        // Process accounts in batches
        int processedAccounts = 0;

        while (processedAccounts < totalAccounts) {
            // Get next batch of accounts using the repository method
            List<Account> batchAccounts = accountRepository.findAccountsForBatchUpdate(batchSize);

            if (batchAccounts.isEmpty()) {
                log.info("No more accounts to process");
                break;
            }

            // Process batch using Virtual Threads
            BatchResult result = processBatchWithVirtualThreads(batchAccounts, processedAccounts / batchSize);

            successCount.addAndGet(result.successCount());
            errorCount.addAndGet(result.errorCount());
            totalProcessingTime.addAndGet(result.processingTimeMs());

            processedAccounts += batchAccounts.size();

            log.info("Processed {}/{} accounts", processedAccounts, totalAccounts);
        }

        // Log final metrics
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(startTime, endTime);

        log.info("Virtual Thread Batch Processing Completed:");
        log.info("  - Total time: {} minutes", duration.toMinutes());
        log.info("  - Accounts processed: {}", successCount.get());
        log.info("  - Errors: {}", errorCount.get());
        log.info("  - Average processing time per account: {} ms",
                successCount.get() > 0 ? totalProcessingTime.get() / successCount.get() : 0);
        log.info("  - Throughput: {} accounts/second",
                successCount.get() > 0 ? (double) successCount.get() / duration.toSeconds() : 0);

        eventPublisher.publishBatchSyncCompleted(successCount.get());
    }

    private BatchResult processBatchWithVirtualThreads(List<Account> accounts, int batchNumber) {
        long batchStartTime = System.currentTimeMillis();
        log.debug("Processing batch {} with {} accounts using Virtual Threads", batchNumber, accounts.size());

        AtomicInteger batchSuccessCount = new AtomicInteger(0);
        AtomicInteger batchErrorCount = new AtomicInteger(0);
        List<String> errors = new CopyOnWriteArrayList<>();

        // Process each account in the batch using virtual threads with StructuredTaskScope
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            List<StructuredTaskScope.Subtask<Boolean>> accountTasks = accounts.stream()
                    .map(account -> scope.fork(() -> processAccountWithVirtualThread(account)))
                    .toList();

            // Wait for all tasks with timeout
            scope.joinUntil(Instant.now().plus(Duration.ofMinutes(5)));

            // Collect results
            for (var task : accountTasks) {
                try {
                    if (task.get()) {
                        batchSuccessCount.incrementAndGet();
                    } else {
                        batchErrorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    batchErrorCount.incrementAndGet();
                    errors.add(e.getMessage());
                }
            }

        } catch (InterruptedException | TimeoutException e) {
            log.error("Batch {} processing interrupted or timed out", batchNumber, e);
            Thread.currentThread().interrupt();
        }

        long batchProcessingTime = System.currentTimeMillis() - batchStartTime;

        log.info("Batch {} completed in {} ms: {} success, {} errors",
                batchNumber, batchProcessingTime, batchSuccessCount.get(), batchErrorCount.get());

        return new BatchResult(
                batchNumber,
                batchSuccessCount.get(),
                batchErrorCount.get(),
                errors,
                batchProcessingTime
        );
    }

    private boolean processAccountWithVirtualThread(Account account) {
        try {
            // Acquire semaphore permit to control concurrent virtual threads
            virtualThreadSemaphore.acquire();

            try {
                return updateAccountWithRetry(account);
            } finally {
                virtualThreadSemaphore.release();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Virtual thread interrupted while processing account {}", account.getAccountId());
            return false;
        }
    }

    private boolean updateAccountWithRetry(Account account) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                if (!domainService.shouldSyncAccount(account)) {
                    log.debug("Account {} recently synced, skipping", account.getAccountId());
                    return true;
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

                // Record metrics
                metricsCollector.recordAccountSynced();

                log.debug("Account {} updated successfully on virtual thread",
                        account.getAccountId());
                return true;

            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    log.error("Failed to update account {} after {} retries: {}",
                            account.getAccountId(), maxRetries, e.getMessage());
                    return false;
                }

                // Exponential backoff with virtual thread
                try {
                    Thread.sleep(Duration.ofSeconds((long) Math.pow(2, retryCount)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }

    private record BatchResult(
            int batchNumber,
            int successCount,
            int errorCount,
            List<String> errors,
            long processingTimeMs
    ) {}
}