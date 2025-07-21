package br.com.openfinance.accounts.application.service;

import br.com.openfinance.accounts.application.dto.AccountUpdateResult;
import br.com.openfinance.accounts.application.dto.AccountUpdateResult.BatchResult;
import br.com.openfinance.accounts.application.event.AccountUpdateEvent;
import br.com.openfinance.accounts.domain.entity.Account;
import br.com.openfinance.accounts.domain.port.AccountRepository;
import br.com.openfinance.accounts.domain.usecase.AccountService;
import br.com.openfinance.core.metrics.OpenFinanceMetrics;
import br.com.openfinance.core.processor.ParallelProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountUpdateOrchestrator {

    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final ReactiveKafkaProducerTemplate<String, AccountUpdateEvent> kafkaTemplate;
    private final OpenFinanceMetrics metrics;

    @Value("${openfinance.accounts.update.batch-size:1000}")
    private int batchSize;

    @Value("${openfinance.accounts.update.parallelism:100}")
    private int parallelism;

    @Value("${openfinance.accounts.update.timeout:30}")
    private int timeoutSeconds;

    public Mono<AccountUpdateResult> orchestrateAccountUpdates() {
        String executionId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger batchCounter = new AtomicInteger(0);

        AccountUpdateResult result = AccountUpdateResult.builder()
                .executionId(executionId)
                .startTime(startTime)
                .build();

        ParallelProcessor<Account, Account> processor = new ParallelProcessor<>(
                parallelism,
                batchSize,
                Duration.ofSeconds(timeoutSeconds)
        );

        return accountRepository.findAccountsForUpdate(1_000_000)
                .buffer(batchSize)
                .flatMap(batch -> {
                    int batchNumber = batchCounter.incrementAndGet();
                    long batchStartTime = System.currentTimeMillis();

                    return processBatch(batch, processor, processedCount, errorCount)
                            .then(Mono.fromCallable(() -> {
                                long processingTime = System.currentTimeMillis() - batchStartTime;

                                BatchResult batchResult = BatchResult.builder()
                                        .batchNumber(batchNumber)
                                        .batchSize(batch.size())
                                        .successCount(processedCount.get())
                                        .errorCount(errorCount.get())
                                        .processingTimeMs(processingTime)
                                        .processedAt(LocalDateTime.now())
                                        .build();

                                result.addBatchResult(batchResult);

                                log.info("Batch {} processed: {} items in {}ms",
                                        batchNumber, batch.size(), processingTime);

                                return batchResult;
                            }));
                })
                .then(Mono.fromCallable(() -> {
                    result.complete();
                    return result;
                }))
                .doFinally(signal -> {
                    processor.shutdown();
                    log.info("Account update orchestration completed. Total processed: {}, Errors: {}",
                            processedCount.get(), errorCount.get());
                })
                .doOnError(error -> {
                    log.error("Account update orchestration failed", error);
                    result.fail(error.getMessage());
                });
    }

    private Mono<Void> processBatch(
            List<Account> batch,
            ParallelProcessor<Account, Account> processor,
            AtomicInteger processedCount,
            AtomicInteger errorCount) {

        return processor.processInParallel(batch, account ->
                        accountService.updateAccountData(account)
                                .flatMap(updatedAccount ->
                                        publishToKafka(updatedAccount)
                                                .thenReturn(updatedAccount) // Retorna o account após publicar no Kafka
                                )
                                .doOnSuccess(result -> processedCount.incrementAndGet())
                                .doOnError(error -> {
                                    errorCount.incrementAndGet();
                                    log.error("Failed to update account {}: {}", account.getAccountId(), error.getMessage());
                                })
                                .onErrorResume(error -> Mono.empty())
                )
                .then();
    }

    private Mono<SenderResult<Void>> publishToKafka(Account account) {
        AccountUpdateEvent event = AccountUpdateEvent.builder()
                .accountId(account.getAccountId())
                .clientId(account.getClientId())
                .institutionId(account.getInstitutionId())
                .balance(account.getBalance())
                .limit(account.getLimit())
                .timestamp(LocalDateTime.now())
                .build();

        return kafkaTemplate.send("account-updates", account.getAccountId(), event)
                .doOnSuccess(result -> log.debug("Published update event for account {}", account.getAccountId()))
                .doOnError(error -> log.error("Failed to publish event for account {}: {}",
                        account.getAccountId(), error.getMessage()));
    }
}
