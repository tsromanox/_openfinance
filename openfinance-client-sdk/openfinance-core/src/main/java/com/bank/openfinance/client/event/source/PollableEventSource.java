package com.bank.openfinance.client.event.source;

import com.bank.openfinance.client.event.model.EventEnvelope;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Interface for event sources that support polling.
 */
public interface PollableEventSource extends EventSource {

    /**
     * Poll for a single event.
     *
     * @param timeout Maximum time to wait for an event
     * @return Optional containing the event if available
     */
    Optional<EventEnvelope<?>> poll(Duration timeout);

    /**
     * Poll for multiple events.
     *
     * @param maxEvents Maximum number of events to retrieve
     * @param timeout Maximum time to wait
     * @return List of available events (may be empty)
     */
    List<EventEnvelope<?>> pollBatch(int maxEvents, Duration timeout);

    /**
     * Get the current queue size (if applicable).
     *
     * @return Number of pending events, or -1 if unknown
     */
    default long getPendingEventCount() {
        return -1;
    }

    /**
     * Set the polling interval.
     *
     * @param interval Polling interval
     */
    void setPollingInterval(Duration interval);
}
