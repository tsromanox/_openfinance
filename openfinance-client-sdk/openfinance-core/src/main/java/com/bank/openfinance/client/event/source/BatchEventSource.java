package com.bank.openfinance.client.event.source;

import com.bank.openfinance.client.event.model.EventEnvelope;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/**
 * Interface for event sources that support batch consumption.
 */
public interface BatchEventSource extends EventSource {

    /**
     * Consume events in batches (blocking).
     *
     * @param batchSize Maximum number of events per batch
     * @param timeout Maximum time to wait for a full batch
     * @return Stream of event batches
     */
    Stream<List<EventEnvelope<?>>> consumeBatchBlocking(int batchSize, Duration timeout);

    /**
     * Consume events in batches (reactive).
     *
     * @param batchSize Maximum number of events per batch
     * @param timeout Maximum time to wait for a full batch
     * @return Flux of event batches
     */
    Flux<List<EventEnvelope<?>>> consumeBatchReactive(int batchSize, Duration timeout);

    /**
     * Acknowledge a batch of events.
     *
     * @param envelopes List of successfully processed events
     */
    void acknowledgeBatch(List<EventEnvelope<?>> envelopes);

    /**
     * Report errors for a batch of events.
     *
     * @param failures Map of event envelopes to their errors
     */
    void reportBatchErrors(List<EventProcessingFailure> failures);
}
