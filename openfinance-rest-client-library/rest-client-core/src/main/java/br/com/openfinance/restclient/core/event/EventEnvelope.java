package br.com.openfinance.restclient.core.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Generic event envelope that encapsulates events from different sources
 * (database or Kafka) in a consistent format for REST API consumption.
 * 
 * This envelope provides:
 * - Event metadata (id, timestamp, source, type)
 * - Payload with generic type support
 * - Correlation tracking
 * - Retry information
 * - Headers for additional context
 * 
 * @param <T> The type of the event payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = EventEnvelope.DatabaseEvent.class, name = "DATABASE"),
    @JsonSubTypes.Type(value = EventEnvelope.KafkaEvent.class, name = "KAFKA")
})
public class EventEnvelope<T> {

    /**
     * Unique identifier for the event
     */
    private String eventId;

    /**
     * Type of the event (e.g., ACCOUNT_UPDATE, CONSENT_CREATED)
     */
    private String eventType;

    /**
     * Source of the event (DATABASE, KAFKA)
     */
    private EventSource source;

    /**
     * Timestamp when the event was created
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime timestamp;

    /**
     * The actual event payload
     */
    private T payload;

    /**
     * Correlation ID for tracking requests across services
     */
    private String correlationId;

    /**
     * Version of the event schema
     */
    private String version;

    /**
     * Current retry attempt (starts from 0)
     */
    private int retryAttempt;

    /**
     * Maximum retry attempts allowed
     */
    private int maxRetryAttempts;

    /**
     * Additional metadata/headers
     */
    private Map<String, Object> headers;

    /**
     * Priority level for event processing
     */
    private EventPriority priority;

    /**
     * API endpoint this event should trigger
     */
    private String targetEndpoint;

    /**
     * HTTP method to use for the API call
     */
    private String httpMethod;

    /**
     * Partition key for Kafka events (helps with ordering)
     */
    private String partitionKey;

    public enum EventSource {
        DATABASE, KAFKA
    }

    public enum EventPriority {
        LOW, NORMAL, HIGH, CRITICAL
    }

    /**
     * Check if event can be retried
     */
    public boolean canRetry() {
        return retryAttempt < maxRetryAttempts;
    }

    /**
     * Increment retry attempt
     */
    public void incrementRetry() {
        this.retryAttempt++;
    }

    /**
     * Check if event has expired based on timestamp and TTL
     */
    public boolean isExpired(long ttlSeconds) {
        return timestamp.plusSeconds(ttlSeconds).isBefore(OffsetDateTime.now());
    }

    /**
     * Create a new envelope with incremented retry
     */
    public EventEnvelope<T> withIncrementedRetry() {
        return EventEnvelope.<T>builder()
                .eventId(this.eventId)
                .eventType(this.eventType)
                .source(this.source)
                .timestamp(this.timestamp)
                .payload(this.payload)
                .correlationId(this.correlationId)
                .version(this.version)
                .retryAttempt(this.retryAttempt + 1)
                .maxRetryAttempts(this.maxRetryAttempts)
                .headers(this.headers)
                .priority(this.priority)
                .targetEndpoint(this.targetEndpoint)
                .httpMethod(this.httpMethod)
                .partitionKey(this.partitionKey)
                .build();
    }

    /**
     * Database-specific event envelope
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseEvent<T> extends EventEnvelope<T> {
        private String tableName;
        private String primaryKey;
        private String operation; // INSERT, UPDATE, DELETE
        private Long sequenceNumber;
    }

    /**
     * Kafka-specific event envelope
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KafkaEvent<T> extends EventEnvelope<T> {
        private String topic;
        private Integer partition;
        private Long offset;
        private String key;
    }
}