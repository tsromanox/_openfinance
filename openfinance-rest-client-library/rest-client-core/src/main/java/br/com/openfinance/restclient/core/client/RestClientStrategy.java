package br.com.openfinance.restclient.core.client;

/**
 * Enum defining the available REST client implementation strategies.
 * Allows developers to choose between reactive and imperative approaches
 * based on their application requirements.
 */
public enum RestClientStrategy {
    /**
     * Reactive implementation using Spring WebClient
     * - Non-blocking I/O
     * - Backpressure support
     * - Reactive streams (Mono/Flux)
     * - Lower memory footprint under high load
     * - Best for applications already using reactive stack
     */
    REACTIVE,

    /**
     * Imperative implementation using Spring RestClient with Virtual Threads
     * - Blocking I/O with virtual thread parallelism
     * - Traditional programming model
     * - Massive concurrency with low overhead
     * - Best for applications using traditional Spring MVC stack
     */
    IMPERATIVE
}