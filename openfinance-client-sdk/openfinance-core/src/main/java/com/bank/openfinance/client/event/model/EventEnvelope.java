package com.bank.openfinance.client.event.model;

import lombok.Builder;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope<T> {

    // Metadata
    @JsonProperty("event_id")
    private final UUID eventId;

    @JsonProperty("event_type")
    private final String eventType;

    @JsonProperty("source")
    private final String source;

    @JsonProperty("timestamp")
    private final Instant timestamp;

    @JsonProperty("correlation_id")
    private final String correlationId;

    @JsonProperty("version")
    @Builder.Default
    private final Integer version = 1;

    // Payload
    @JsonProperty("payload")
    private final T payload;

    // Context
    @JsonProperty("headers")
    private final Map<String, Object> headers;

    @JsonProperty("tenant_id")
    private final String tenantId;

    @JsonProperty("user_id")
    private final String userId;

    // Processing
    @JsonProperty("retry_count")
    @Builder.Default
    private final Integer retryCount = 0;

    @JsonProperty("process_after")
    private final Instant processAfter;

    @JsonProperty("priority")
    @Builder.Default
    private final EventPriority priority = EventPriority.NORMAL;

    public enum EventPriority {
        LOW(0), NORMAL(1), HIGH(2), CRITICAL(3);

        private final int value;

        EventPriority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    // Factory methods
    public static <T> EventEnvelope<T> of(T payload) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID())
                .timestamp(Instant.now())
                .payload(payload)
                .build();
    }

    public static <T> EventEnvelope<T> of(String eventType, T payload) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .timestamp(Instant.now())
                .payload(payload)
                .build();
    }
}
