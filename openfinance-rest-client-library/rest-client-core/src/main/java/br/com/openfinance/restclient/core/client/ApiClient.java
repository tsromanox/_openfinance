package br.com.openfinance.restclient.core.client;

import br.com.openfinance.restclient.core.config.ClientConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

/**
 * Generic interface for REST API clients supporting both reactive and imperative patterns.
 * Implementations will handle the specific HTTP client technology (WebClient vs RestClient).
 * 
 * @param <T> The response type
 */
public interface ApiClient<T> {

    /**
     * Reactive GET request
     * @param endpoint The API endpoint
     * @param responseType The expected response type
     * @return Mono with the response
     */
    <R> Mono<R> getReactive(String endpoint, Class<R> responseType);

    /**
     * Reactive POST request
     * @param endpoint The API endpoint  
     * @param body Request body
     * @param responseType The expected response type
     * @return Mono with the response
     */
    <R> Mono<R> postReactive(String endpoint, Object body, Class<R> responseType);

    /**
     * Reactive PUT request
     * @param endpoint The API endpoint
     * @param body Request body
     * @param responseType The expected response type
     * @return Mono with the response
     */
    <R> Mono<R> putReactive(String endpoint, Object body, Class<R> responseType);

    /**
     * Reactive DELETE request
     * @param endpoint The API endpoint
     * @param responseType The expected response type
     * @return Mono with the response
     */
    <R> Mono<R> deleteReactive(String endpoint, Class<R> responseType);

    /**
     * Reactive batch GET requests
     * @param endpoints List of endpoints to call
     * @param responseType The expected response type
     * @return Flux with responses
     */
    <R> Flux<R> getBatchReactive(Flux<String> endpoints, Class<R> responseType);

    /**
     * Imperative GET request (using Virtual Threads)
     * @param endpoint The API endpoint
     * @param responseType The expected response type
     * @return CompletableFuture with the response
     */
    <R> CompletableFuture<R> getImperative(String endpoint, Class<R> responseType);

    /**
     * Imperative POST request (using Virtual Threads)
     * @param endpoint The API endpoint
     * @param body Request body
     * @param responseType The expected response type
     * @return CompletableFuture with the response
     */
    <R> CompletableFuture<R> postImperative(String endpoint, Object body, Class<R> responseType);

    /**
     * Imperative PUT request (using Virtual Threads)
     * @param endpoint The API endpoint
     * @param body Request body
     * @param responseType The expected response type
     * @return CompletableFuture with the response
     */
    <R> CompletableFuture<R> putImperative(String endpoint, Object body, Class<R> responseType);

    /**
     * Imperative DELETE request (using Virtual Threads)
     * @param endpoint The API endpoint
     * @param responseType The expected response type
     * @return CompletableFuture with the response
     */
    <R> CompletableFuture<R> deleteImperative(String endpoint, Class<R> responseType);

    /**
     * Imperative batch GET requests (using Virtual Threads)
     * @param endpoints List of endpoints to call
     * @param responseType The expected response type
     * @return CompletableFuture with list of responses
     */
    <R> CompletableFuture<java.util.List<R>> getBatchImperative(
        java.util.List<String> endpoints, Class<R> responseType);

    /**
     * Get client configuration
     * @return Current client configuration
     */
    ClientConfiguration getConfiguration();

    /**
     * Health check for the API client
     * @return Mono<Boolean> indicating if client is healthy
     */
    Mono<Boolean> healthCheck();
}