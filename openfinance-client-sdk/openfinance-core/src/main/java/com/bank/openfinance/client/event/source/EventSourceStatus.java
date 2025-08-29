package com.bank.openfinance.client.event.source;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

/**
 * Status information for an event source.
 */
@Data
@Builder
public class EventSourceStatus {
    private final boolean healthy;
    private final EventSourceType sourceType;
    private final Long eventsConsumed;
    private final Long eventsProcessed;
    private final Long eventsFailed;
    private final Instant lastEventTime;
    private final Instant startTime;
    private final String currentState;
    private final Map<String, Object> metadata;
    private final String errorMessage;
}