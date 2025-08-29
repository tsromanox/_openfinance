package br.com.openfinance.restclient.events.consumer;

import br.com.openfinance.restclient.core.event.EventEnvelope;
import reactor.core.publisher.Flux;

/**
 * Generic interface for consuming events from different sources.
 * Implementations will handle specific sources like database polling or Kafka consumption.
 * 
 * @param <T> The type of event payload
 */
public interface EventConsumer<T> {

    /**
     * Start consuming events
     * @return Flux of EventEnvelopes
     */
    Flux<EventEnvelope<T>> consume();

    /**
     * Stop consuming events
     */
    void stop();

    /**
     * Check if consumer is running
     * @return true if consumer is actively consuming events
     */
    boolean isRunning();

    /**
     * Get consumer configuration
     * @return Consumer-specific configuration
     */
    EventConsumerConfiguration getConfiguration();
}