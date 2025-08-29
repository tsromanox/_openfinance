package com.bank.openfinance.client.event.source;

import com.bank.openfinance.client.event.model.EventEnvelope;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

/**
 * Represents a failure in processing an event.
 */
@Data
@Builder
public class EventProcessingFailure {
    private final EventEnvelope<?> envelope;
    private final Throwable error;
    private final Instant failureTime;
    private final int attemptNumber;
    private final boolean retryable;
    private final String failureReason;
}
