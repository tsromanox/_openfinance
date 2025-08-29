package com.bank.openfinance.client.api;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ClientApi<T,M>{

    // Reactive methods
    Mono<T> getAccountReactive(String accountId);
    Flux<M> getTransactionsReactive(String accountId, int page, int pageSize);

    // Async methods (Virtual Threads)
    CompletableFuture<T> getAccountAsync(String accountId);
    CompletableFuture<List<T>> getTransactionsAsync(String accountId, int page, int pageSize);

    // Blocking methods
    T getAccount(String accountId);
    List<M> getTransactions(String accountId, int page, int pageSize);

    // Batch operations
    default List<T> getAccountsBatch(List<String> accountIds) {
        return accountIds.stream()
                .map(this::getAccount)
                .toList();
    }

    default Flux<T> getAccountsBatchReactive(List<String> accountIds) {
        return Flux.fromIterable(accountIds)
                .flatMap(this::getAccountReactive);
    }
}