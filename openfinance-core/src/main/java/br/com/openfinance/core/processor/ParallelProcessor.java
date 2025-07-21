package br.com.openfinance.core.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public class ParallelProcessor<T, R> {

    private final int parallelism;
    private final int batchSize;
    private final Duration timeout;
    private final ExecutorService virtualThreadExecutor;

    public ParallelProcessor(int parallelism, int batchSize, Duration timeout) {
        this.parallelism = parallelism;
        this.batchSize = batchSize;
        this.timeout = timeout;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public Flux<R> processInParallel(List<T> items, Function<T, Mono<R>> processor) {
        return Flux.fromIterable(items)
                .window(batchSize)
                .parallel(parallelism)
                .runOn(Schedulers.fromExecutor(virtualThreadExecutor))
                .flatMap(batch ->
                        batch.flatMap(item ->
                                processor.apply(item)
                                        .timeout(timeout)
                                        .onErrorResume(error -> {
                                            log.error("Error processing item: {}", error.getMessage());
                                            return Mono.empty();
                                        })
                        )
                )
                .sequential();
    }

    public CompletableFuture<List<R>> processAllAsync(
            List<T> items,
            Function<T, CompletableFuture<R>> processor) {

        List<CompletableFuture<R>> futures = items.stream()
                .map(item ->
                        CompletableFuture.supplyAsync(() ->
                                        processor.apply(item).join(), virtualThreadExecutor)
                                .exceptionally(throwable -> {
                                    log.error("Error processing item", throwable);
                                    return null;
                                })
                )
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(result -> result != null)
                        .toList()
                );
    }

    public void shutdown() {
        virtualThreadExecutor.shutdown();
    }
}

