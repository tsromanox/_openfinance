package br.com.openfinance.infrastructure.persistence.service;

import br.com.openfinance.domain.account.Account;
import br.com.openfinance.domain.consent.Consent;
import br.com.openfinance.infrastructure.persistence.repository.reactive.ReactiveConsentRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * High-performance data synchronization service using Virtual Threads, 
 * Structured Concurrency, and reactive streaming for maximum throughput.
 */
@Slf4j
@Service
public class ParallelDataSyncService {
    
    private final ReactiveConsentRepository reactiveConsentRepository;
    private final Executor virtualThreadExecutor;
    private final MeterRegistry meterRegistry;
    
    // Performance metrics
    private final Timer syncDurationTimer;
    private final Counter syncSuccessCounter;
    private final Counter syncErrorCounter;
    private final AtomicLong activeSyncOperations;
    
    // Configuration
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_CONCURRENT_SYNCS = 50;
    private static final Duration SYNC_TIMEOUT = Duration.ofMinutes(5);
    
    public ParallelDataSyncService(
            ReactiveConsentRepository reactiveConsentRepository,
            MeterRegistry meterRegistry) {
        
        this.reactiveConsentRepository = reactiveConsentRepository;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.meterRegistry = meterRegistry;
        this.activeSyncOperations = new AtomicLong(0);
        
        // Initialize metrics
        this.syncDurationTimer = Timer.builder("data.sync.duration")
                .description("Time taken for data synchronization operations")
                .register(meterRegistry);
        this.syncSuccessCounter = Counter.builder("data.sync.success")
                .description("Number of successful sync operations")
                .register(meterRegistry);
        this.syncErrorCounter = Counter.builder("data.sync.errors")
                .description("Number of failed sync operations")
                .register(meterRegistry);
        
        // Register gauge for active operations
        meterRegistry.gauge("data.sync.active.operations", activeSyncOperations, AtomicLong::get);
    }
    
    /**
     * Reactive batch synchronization with backpressure control.
     */
    public Flux<SyncResult> syncConsentsReactive(int batchSize) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        
        return reactiveConsentRepository.findConsentsNeedingSync(threshold, batchSize)
                .limitRate(10) // Control backpressure
                .parallel(5) // Process in parallel
                .runOn(Schedulers.parallel())
                .flatMap(this::syncConsentReactive)
                .doOnNext(result -> {
                    if (result.success()) {
                        syncSuccessCounter.increment();
                    } else {
                        syncErrorCounter.increment();
                    }
                })
                .sequential();
    }
    
    /**
     * Structured Concurrency implementation for batch consent synchronization.
     */
    public CompletableFuture<List<SyncResult>> syncConsentsWithStructuredConcurrency(
            List<Consent> consents) {
        
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.Sample.start(meterRegistry);
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                activeSyncOperations.addAndGet(consents.size());
                
                // Fork sync tasks for each consent
                List<StructuredTaskScope.Subtask<SyncResult>> syncTasks = consents.stream()
                        .map(consent -> scope.fork(() -> syncConsentBlocking(consent)))
                        .toList();
                
                // Wait for all tasks to complete or fail fast
                scope.join();
                scope.throwIfFailed();
                
                // Collect results
                List<SyncResult> results = syncTasks.stream()
                        .map(StructuredTaskScope.Subtask::get)
                        .toList();
                
                long successful = results.stream().mapToLong(r -> r.success() ? 1 : 0).sum();
                long failed = results.size() - successful;
                
                syncSuccessCounter.increment(successful);
                syncErrorCounter.increment(failed);
                
                log.info("Synchronized {} consents - Success: {}, Failed: {}", 
                        results.size(), successful, failed);
                
                return results;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Sync operation interrupted", e);
            } finally {
                activeSyncOperations.addAndGet(-consents.size());
                sample.stop(syncDurationTimer);
            }
        }, virtualThreadExecutor);
    }
    
    /**
     * High-throughput parallel processing with adaptive batch sizing.
     */
    public CompletableFuture<List<SyncResult>> adaptiveBatchSync(
            Function<Integer, List<Consent>> consentFetcher,
            int initialBatchSize) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<SyncResult> allResults = new java.util.ArrayList<>();
            int currentBatchSize = initialBatchSize;
            long lastBatchTime = 0;
            
            while (true) {
                List<Consent> batch = consentFetcher.apply(currentBatchSize);
                if (batch.isEmpty()) {
                    break;
                }
                
                long batchStartTime = System.currentTimeMillis();
                
                try {
                    List<SyncResult> batchResults = syncConsentsWithStructuredConcurrency(batch).get();
                    allResults.addAll(batchResults);
                    
                    long batchDuration = System.currentTimeMillis() - batchStartTime;
                    
                    // Adaptive batch size adjustment based on performance
                    if (lastBatchTime > 0) {
                        double performanceRatio = (double) batchDuration / lastBatchTime;
                        
                        if (performanceRatio > 1.5 && currentBatchSize > 20) {
                            currentBatchSize = Math.max(20, currentBatchSize - 20);
                            log.debug("Reduced batch size to {} due to performance degradation", currentBatchSize);
                        } else if (performanceRatio < 0.8 && currentBatchSize < 200) {
                            currentBatchSize = Math.min(200, currentBatchSize + 20);
                            log.debug("Increased batch size to {} due to good performance", currentBatchSize);
                        }
                    }
                    
                    lastBatchTime = batchDuration;
                    
                } catch (Exception e) {
                    log.error("Error in adaptive batch sync: {}", e.getMessage(), e);
                    break;
                }
            }
            
            log.info("Adaptive batch sync completed. Processed {} total records", allResults.size());
            return allResults;
            
        }, virtualThreadExecutor);
    }
    
    /**
     * Parallel account synchronization with rate limiting.
     */
    public CompletableFuture<List<Account>> syncAccountsParallel(
            List<String> accountIds,
            Function<String, Account> accountFetcher) {
        
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Fork account fetch tasks with concurrency limit
                List<StructuredTaskScope.Subtask<Account>> accountTasks = accountIds.stream()
                        .limit(MAX_CONCURRENT_SYNCS)
                        .map(accountId -> scope.fork(() -> {
                            try {
                                Thread.sleep(10); // Simple rate limiting
                                return accountFetcher.apply(accountId);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException("Account sync interrupted", e);
                            }
                        }))
                        .toList();
                
                scope.join();
                scope.throwIfFailed();
                
                return accountTasks.stream()
                        .map(StructuredTaskScope.Subtask::get)
                        .toList();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Parallel account sync interrupted", e);
            }
        }, virtualThreadExecutor);
    }
    
    /**
     * Reactive consent synchronization with timeout and retry.
     */
    private Mono<SyncResult> syncConsentReactive(
            br.com.openfinance.infrastructure.persistence.repository.reactive.ReactiveConsentEntity consentEntity) {
        
        return Mono.fromCallable(() -> {
            // Simulate sync operation
            Thread.sleep(100); // Mock external API call
            return new SyncResult(consentEntity.id(), true, null);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .timeout(SYNC_TIMEOUT)
        .onErrorResume(error -> {
            log.error("Error syncing consent {}: {}", consentEntity.id(), error.getMessage());
            return Mono.just(new SyncResult(consentEntity.id(), false, error.getMessage()));
        });
    }
    
    private SyncResult syncConsentBlocking(Consent consent) {
        try {
            // Simulate blocking sync operation
            Thread.sleep(50);
            return new SyncResult(consent.getId(), true, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SyncResult(consent.getId(), false, "Interrupted");
        } catch (Exception e) {
            return new SyncResult(consent.getId(), false, e.getMessage());
        }
    }
    
    /**
     * Get current synchronization performance metrics.
     */
    public SyncPerformanceMetrics getPerformanceMetrics() {
        return new SyncPerformanceMetrics(
                activeSyncOperations.get(),
                (long) syncSuccessCounter.count(),
                (long) syncErrorCounter.count(),
                syncDurationTimer.mean(java.util.concurrent.TimeUnit.SECONDS),
                syncDurationTimer.max(java.util.concurrent.TimeUnit.SECONDS)
        );
    }
    
    // Result records
    public record SyncResult(
            java.util.UUID consentId,
            boolean success,
            String errorMessage
    ) {}
    
    public record SyncPerformanceMetrics(
            long activeOperations,
            long totalSuccessful,
            long totalFailed,
            double averageDurationSeconds,
            double maxDurationSeconds
    ) {}
}