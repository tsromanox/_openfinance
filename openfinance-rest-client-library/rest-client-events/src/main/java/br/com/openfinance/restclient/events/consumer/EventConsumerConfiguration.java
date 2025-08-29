package br.com.openfinance.restclient.events.consumer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration for event consumers (database and Kafka)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventConsumerConfiguration {

    /**
     * Event source type
     */
    private EventSourceType sourceType;

    /**
     * Polling interval for database consumers
     */
    @Builder.Default
    private Duration pollingInterval = Duration.ofSeconds(5);

    /**
     * Batch size for processing events
     */
    @Builder.Default
    private int batchSize = 100;

    /**
     * Maximum events to keep in buffer
     */
    @Builder.Default
    private int bufferSize = 1000;

    /**
     * Enable retry for failed events
     */
    @Builder.Default
    private boolean retryEnabled = true;

    /**
     * Maximum retry attempts
     */
    @Builder.Default
    private int maxRetryAttempts = 3;

    /**
     * Retry delay
     */
    @Builder.Default
    private Duration retryDelay = Duration.ofSeconds(10);

    /**
     * Database-specific configuration
     */
    private DatabaseConfiguration database;

    /**
     * Kafka-specific configuration
     */
    private KafkaConfiguration kafka;

    /**
     * Additional properties
     */
    private Map<String, Object> properties;

    public enum EventSourceType {
        DATABASE, KAFKA
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseConfiguration {
        /**
         * Table name to poll
         */
        private String tableName;

        /**
         * Column name for event ID
         */
        @Builder.Default
        private String idColumn = "id";

        /**
         * Column name for timestamp
         */
        @Builder.Default
        private String timestampColumn = "created_at";

        /**
         * Column name for processed flag
         */
        @Builder.Default
        private String processedColumn = "processed";

        /**
         * Column name for retry count
         */
        @Builder.Default
        private String retryCountColumn = "retry_count";

        /**
         * WHERE clause for filtering events
         */
        @Builder.Default
        private String whereClause = "processed = false";

        /**
         * ORDER BY clause
         */
        @Builder.Default
        private String orderByClause = "created_at ASC";

        /**
         * Use reactive database operations (R2DBC)
         */
        @Builder.Default
        private boolean reactive = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KafkaConfiguration {
        /**
         * Kafka topic name
         */
        private String topic;

        /**
         * Consumer group ID
         */
        private String groupId;

        /**
         * Bootstrap servers
         */
        private String bootstrapServers;

        /**
         * Auto offset reset strategy
         */
        @Builder.Default
        private String autoOffsetReset = "earliest";

        /**
         * Enable auto commit
         */
        @Builder.Default
        private boolean enableAutoCommit = false;

        /**
         * Session timeout
         */
        @Builder.Default
        private Duration sessionTimeout = Duration.ofSeconds(30);

        /**
         * Poll timeout
         */
        @Builder.Default
        private Duration pollTimeout = Duration.ofSeconds(1);

        /**
         * Use reactive Kafka consumer
         */
        @Builder.Default
        private boolean reactive = true;

        /**
         * Additional Kafka properties
         */
        private Map<String, Object> additionalProperties;
    }
}