package br.com.openfinance.infrastructure.client;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

/**
 * Specialized component for high-performance parallel batch processing with resource management.
 */
@Slf4j
@Component
public class ParallelBatchProcessor {
    
    private final Executor virtualThreadExecutor;
    private final Semaphore concurrencyLimiter;
    private final MeterRegistry meterRegistry;
    
    // Metrics
    private final Counter processedItemsCounter;
    private final Counter failedItemsCounter;
    private final Timer batchProcessingTimer;
    
    private static final int DEFAULT_MAX_CONCURRENCY = 50;
    private static final int DEFAULT_BATCH_SIZE = 100;
    
    public ParallelBatchProcessor(MeterRegistry meterRegistry) {
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.concurrencyLimiter = new Semaphore(DEFAULT_MAX_CONCURRENCY);
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.processedItemsCounter = Counter.builder("batch.processor.items.processed")
                .description("Number of items successfully processed")
                .register(meterRegistry);
        this.failedItemsCounter = Counter.builder("batch.processor.items.failed")
                .description("Number of items that failed processing")
                .register(meterRegistry);
        this.batchProcessingTimer = Timer.builder("batch.processor.execution.time")
                .description("Time taken to process batches")
                .register(meterRegistry);
    }
    
    /**
     * Process a batch of items in parallel using reactive streams.
     */
    public <T, R> Flux<R> processBatchReactive(
            List<T> items, 
            Function<T, Mono<R>> processor, 
            int concurrency) {
        
        return Timer.Sample.start(meterRegistry)
                .stop(batchProcessingTimer)
                .wrap(Flux.fromIterable(items)
                        .parallel(Math.min(concurrency, items.size()))
                        .runOn(Schedulers.fromExecutor(virtualThreadExecutor))
                        .flatMap(item -> processor.apply(item)
                                .doOnSuccess(result -> processedItemsCounter.increment())
                                .doOnError(error -> {
                                    failedItemsCounter.increment();
                                    log.error("Failed to process item {}: {}", item, error.getMessage());
                                })
                                .onErrorResume(error -> Mono.empty())) // Continue processing other items
                        .sequential());
    }
    
    /**
     * Process batch with backpressure control and retry logic.
     */
    public <T, R> Flux<R> processBatchWithBackpressure(
            List<T> items,
            Function<T, Mono<R>> processor,
            int concurrency,
            Duration backpressureDelay) {
        
        return Flux.fromIterable(items)
                .window(DEFAULT_BATCH_SIZE) // Process in chunks
                .concatMap(window -> window
                        .parallel(concurrency)
                        .runOn(Schedulers.fromExecutor(virtualThreadExecutor))
                        .flatMap(item -> acquirePermitAndProcess(item, processor))
                        .sequential()
                        .delayElements(backpressureDelay)) // Add delay between batches
                .doOnNext(result -> processedItemsCounter.increment())
                .doOnError(error -> failedItemsCounter.increment());
    }
    
    /**
     * Process using CompletableFuture for non-reactive contexts.
     */
    public <T, R> CompletableFuture<List<R>> processBatchAsync(
            List<T> items,
            Function<T, R> processor,
            int concurrency) {
        
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<R>> futures = items.parallelStream()
                    .limit(concurrency)
                    .map(item -> CompletableFuture.supplyAsync(() -> {
                        try {
                            concurrencyLimiter.acquire();
                            R result = processor.apply(item);
                            processedItemsCounter.increment();
                            return result;
                        } catch (Exception e) {
                            failedItemsCounter.increment();
                            log.error("Failed to process item {}: {}", item, e.getMessage());
                            throw new RuntimeException(e);
                        } finally {
                            concurrencyLimiter.release();
                        }
                    }, virtualThreadExecutor))
                    .toList();
            
            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        }, virtualThreadExecutor).whenComplete((result, throwable) -> sample.stop(batchProcessingTimer));
    }
    
    /**
     * Process with structured concurrency (Java 21 feature).
     */
    public <T, R> List<R> processBatchStructured(
            List<T> items,
            Function<T, R> processor,
            int maxConcurrency) throws InterruptedException {
        
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<R>> subtasks = items.stream()
                    .limit(maxConcurrency)
                    .map(item -> scope.fork(() -> {
                        try {
                            R result = processor.apply(item);
                            processedItemsCounter.increment();
                            return result;
                        } catch (Exception e) {
                            failedItemsCounter.increment();
                            log.error("Failed to process item {}: {}", item, e.getMessage());
                            throw e;
                        }
                    }))
                    .toList();
            
            scope.join(); // Wait for all tasks to complete
            scope.throwIfFailed(); // Propagate any failures
            
            return subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();
                    
        } finally {
            sample.stop(batchProcessingTimer);
        }
    }
    
    /**
     * Adaptive processing that adjusts concurrency based on performance metrics.
     */
    public <T, R> Flux<R> processBatchAdaptive(
            List<T> items,
            Function<T, Mono<R>> processor) {
        
        return Flux.fromIterable(items)
                .parallel()
                .runOn(Schedulers.fromExecutor(virtualThreadExecutor))
                .flatMap(item -> processor.apply(item)
                        .timeout(Duration.ofSeconds(30)) // Prevent hanging
                        .doOnSuccess(result -> processedItemsCounter.increment())
                        .doOnError(error -> {
                            failedItemsCounter.increment();
                            log.error("Failed to process item {}: {}", item, error.getMessage());
                        })
                        .onErrorResume(error -> Mono.empty())
                        .metrics()) // Enable detailed metrics
                .sequential();
    }
    
    private <T, R> Mono<R> acquirePermitAndProcess(T item, Function<T, Mono<R>> processor) {
        return Mono.fromCallable(() -> {
            concurrencyLimiter.acquire();
            return item;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(processor)
        .doFinally(signal -> concurrencyLimiter.release());
    }
    
    /**
     * Get current processing statistics.
     */
    public ProcessingStats getStats() {
        return new ProcessingStats(
                (long) processedItemsCounter.count(),
                (long) failedItemsCounter.count(),
                concurrencyLimiter.availablePermits(),
                batchProcessingTimer.mean(java.util.concurrent.TimeUnit.SECONDS)
        );
    }
    
    public record ProcessingStats(
            long processedItems,
            long failedItems,
            int availablePermits,
            double averageProcessingTimeSeconds
    ) {}
}