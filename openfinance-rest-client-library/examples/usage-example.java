// Example usage of the OpenFinance REST Client Library

package com.example.openfinance;

import br.com.openfinance.restclient.core.client.ApiClient;
import br.com.openfinance.restclient.core.event.EventEnvelope;
import br.com.openfinance.restclient.events.consumer.EventConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SpringBootApplication
public class OpenFinanceExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(OpenFinanceExampleApplication.class, args);
    }
}

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final ApiClient<?> apiClient;

    // Reactive approach - recommended for high throughput
    public Mono<AccountDto> getAccount(String accountId) {
        return apiClient.getReactive("/accounts/" + accountId, AccountDto.class)
            .doOnSuccess(account -> log.info("Retrieved account: {}", account.getAccountId()))
            .doOnError(error -> log.error("Failed to get account {}: {}", accountId, error.getMessage()));
    }

    // Imperative approach - familiar blocking model with Virtual Threads
    public CompletableFuture<AccountDto> getAccountAsync(String accountId) {
        return apiClient.getImperative("/accounts/" + accountId, AccountDto.class)
            .thenApply(account -> {
                log.info("Retrieved account: {}", account.getAccountId());
                return account;
            })
            .exceptionally(throwable -> {
                log.error("Failed to get account {}: {}", accountId, throwable.getMessage());
                return null;
            });
    }

    // Batch processing - efficiently handle multiple requests
    public Flux<AccountDto> getMultipleAccounts(List<String> accountIds) {
        List<String> endpoints = accountIds.stream()
            .map(id -> "/accounts/" + id)
            .toList();

        return apiClient.getBatchReactive(Flux.fromIterable(endpoints), AccountDto.class)
            .doOnNext(account -> log.debug("Processed account: {}", account.getAccountId()))
            .onErrorContinue((throwable, obj) -> 
                log.warn("Failed to process account: {}", throwable.getMessage()));
    }

    // Synchronize account balance
    public Mono<BalanceDto> syncAccountBalance(String accountId) {
        return apiClient.postReactive(
            "/accounts/" + accountId + "/sync", 
            new BalanceSyncRequest(accountId), 
            BalanceDto.class
        );
    }

    // Update account data
    public CompletableFuture<Void> updateAccount(String accountId, AccountUpdateDto updateData) {
        return apiClient.putImperative(
            "/accounts/" + accountId, 
            updateData, 
            Void.class
        );
    }
}

@Slf4j
@Service
@RequiredArgsConstructor
public class EventDrivenAccountProcessor {

    private final EventConsumer<AccountEventDto> eventConsumer;
    private final AccountService accountService;

    @EventListener
    public void startEventProcessing() {
        log.info("Starting event-driven account processing...");
        
        eventConsumer.consume()
            .flatMap(this::processAccountEvent)
            .onErrorContinue(this::handleProcessingError)
            .subscribe();
    }

    private Mono<Void> processAccountEvent(EventEnvelope<AccountEventDto> eventEnvelope) {
        var event = eventEnvelope.getPayload();
        
        log.info("Processing event: {} for account: {}", 
                eventEnvelope.getEventType(), event.getAccountId());

        return switch (eventEnvelope.getEventType()) {
            case "ACCOUNT_UPDATE" -> handleAccountUpdate(event);
            case "BALANCE_SYNC" -> handleBalanceSync(event);
            case "ACCOUNT_CREATED" -> handleAccountCreated(event);
            default -> {
                log.warn("Unknown event type: {}", eventEnvelope.getEventType());
                yield Mono.empty();
            }
        };
    }

    private Mono<Void> handleAccountUpdate(AccountEventDto event) {
        return accountService.syncAccountBalance(event.getAccountId())
            .doOnSuccess(balance -> log.info("Updated balance for account {}: {}", 
                    event.getAccountId(), balance.getAvailableAmount()))
            .then();
    }

    private Mono<Void> handleBalanceSync(AccountEventDto event) {
        // Custom sync logic
        return Mono.fromCallable(() -> {
            log.info("Syncing balance for account: {}", event.getAccountId());
            // Perform sync operations
            return null;
        }).then();
    }

    private Mono<Void> handleAccountCreated(AccountEventDto event) {
        return accountService.getAccount(event.getAccountId())
            .doOnSuccess(account -> log.info("Account created: {}", account))
            .then();
    }

    private void handleProcessingError(Throwable error, Object event) {
        log.error("Failed to process event: {}, error: {}", event, error.getMessage());
        // Implement dead letter queue or retry logic here
    }
}

@Slf4j
@Service
@RequiredArgsConstructor
public class HighThroughputBatchProcessor {

    private final ApiClient<?> apiClient;

    /**
     * Process a large batch of accounts using structured concurrency
     * Demonstrates handling 10,000+ accounts efficiently
     */
    public CompletableFuture<BatchProcessingResult> processBatch(List<String> accountIds) {
        log.info("Starting batch processing for {} accounts", accountIds.size());
        
        // Split into smaller chunks for optimal processing
        int chunkSize = 100;
        List<List<String>> chunks = partition(accountIds, chunkSize);
        
        return CompletableFuture.supplyAsync(() -> {
            var results = new BatchProcessingResult();
            var startTime = System.currentTimeMillis();
            
            // Process chunks in parallel using Virtual Threads
            chunks.parallelStream().forEach(chunk -> {
                try {
                    List<String> endpoints = chunk.stream()
                        .map(id -> "/accounts/" + id + "/sync")
                        .toList();
                    
                    // Using imperative approach for this example
                    CompletableFuture<List<SyncResultDto>> chunkResult = 
                        apiClient.getBatchImperative(endpoints, SyncResultDto.class);
                    
                    List<SyncResultDto> chunkResults = chunkResult.join();
                    results.addResults(chunkResults);
                    
                } catch (Exception e) {
                    log.error("Failed to process chunk: {}", e.getMessage());
                    results.addError(e.getMessage());
                }
            });
            
            var endTime = System.currentTimeMillis();
            results.setProcessingTimeMs(endTime - startTime);
            
            log.info("Batch processing completed. Processed: {}, Errors: {}, Time: {}ms",
                    results.getSuccessCount(), results.getErrorCount(), results.getProcessingTimeMs());
            
            return results;
        });
    }

    private <T> List<List<T>> partition(List<T> list, int chunkSize) {
        List<List<T>> chunks = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i += chunkSize) {
            chunks.add(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
        return chunks;
    }
}

// DTOs
record AccountDto(
    String accountId,
    String brandName,
    String companyCnpj,
    String type,
    String compeCode,
    String branchCode,
    String number,
    String checkDigit
) {}

record BalanceDto(
    String availableAmount,
    String availableAmountCurrency,
    String blockedAmount,
    String blockedAmountCurrency
) {}

record AccountEventDto(
    String accountId,
    String customerId,
    String action
) {}

record BalanceSyncRequest(String accountId) {}

record AccountUpdateDto(
    String accountId,
    String updateType,
    Object data
) {}

record SyncResultDto(
    String accountId,
    boolean success,
    String message
) {}

@lombok.Data
class BatchProcessingResult {
    private int successCount = 0;
    private int errorCount = 0;
    private long processingTimeMs = 0;
    private List<String> errors = new java.util.ArrayList<>();
    
    public void addResults(List<SyncResultDto> results) {
        successCount += (int) results.stream()
            .mapToLong(r -> r.success() ? 1 : 0)
            .sum();
        errorCount += (int) results.stream()
            .mapToLong(r -> r.success() ? 0 : 1)
            .sum();
    }
    
    public void addError(String error) {
        errors.add(error);
        errorCount++;
    }
}