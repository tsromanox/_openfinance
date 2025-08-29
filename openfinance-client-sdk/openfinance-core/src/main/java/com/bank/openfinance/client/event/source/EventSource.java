package com.bank.openfinance.client.event.source;

import com.bank.openfinance.client.event.model.EventEnvelope;
import reactor.core.publisher.Flux;
import java.util.stream.Stream;

/**
 * Interface for event sources that can provide events for processing.
 * Supports both reactive (non-blocking) and imperative (blocking) consumption patterns.
 *
 * @author OpenFinance Team
 * @since 1.0.0
 */
public interface EventSource {

    /**
     * Consume events in a blocking manner using Java Streams.
     * Suitable for use with Virtual Threads.
     *
     * @return Stream of event envelopes
     */
    Stream<EventEnvelope<?>> consumeBlocking();

    /**
     * Consume events in a reactive, non-blocking manner.
     * Suitable for use with WebFlux and reactive programming.
     *
     * @return Flux of event envelopes
     */
    Flux<EventEnvelope<?>> consumeReactive();

    /**
     * Start consuming events. Called during initialization.
     */
    default void start() {
        // Default implementation - can be overridden
    }

    /**
     * Stop consuming events. Called during shutdown.
     */
    default void stop() {
        // Default implementation - can be overridden
    }

    /**
     * Check if the event source is healthy and operational.
     *
     * @return true if healthy, false otherwise
     */
    default boolean isHealthy() {
        return true;
    }

    /**
     * Get the current status of the event source.
     *
     * @return EventSourceStatus object containing detailed status information
     */
    default EventSourceStatus getStatus() {
        return EventSourceStatus.builder()
                .healthy(isHealthy())
                .sourceType(getSourceType())
                .build();
    }

    /**
     * Get the type of this event source.
     *
     * @return EventSourceType enum value
     */
    EventSourceType getSourceType();

    /**
     * Acknowledge successful processing of an event.
     *
     * @param envelope The event envelope that was successfully processed
     */
    default void acknowledge(EventEnvelope<?> envelope) {
        // Default implementation - can be overridden
    }

    /**
     * Report failure in processing an event.
     *
     * @param envelope The event envelope that failed processing
     * @param error The error that occurred
     */
    default void reportError(EventEnvelope<?> envelope, Throwable error) {
        // Default implementation - can be overridden
    }
}
