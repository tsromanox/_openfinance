package br.com.openfinance.restclient.starter;

import br.com.openfinance.restclient.events.consumer.EventConsumerConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration properties for event consumers.
 * 
 * Example usage in application.yml:
 * 
 * ```yaml
 * openfinance:
 *   rest-client:
 *     events:
 *       enabled: true
 *       source: database  # or kafka
 *       polling-interval: 5s
 *       batch-size: 100
 *       retry-enabled: true
 *       max-retry-attempts: 3
 *       database:
 *         table-name: api_events
 *         reactive: true
 *       kafka:
 *         topic: api-events
 *         group-id: rest-client-consumer
 *         bootstrap-servers: localhost:9092
 * ```
 */
@Data
@ConfigurationProperties(prefix = "openfinance.rest-client.events")
public class EventConsumerProperties {

    /**
     * Enable event consumption
     */
    private boolean enabled = false;

    /**
     * Event source type (database or kafka)
     */
    private String source = "database";

    /**
     * Polling interval for database consumers
     */
    private Duration pollingInterval = Duration.ofSeconds(5);

    /**
     * Batch size for processing events
     */
    private int batchSize = 100;

    /**
     * Maximum events to keep in buffer
     */
    private int bufferSize = 1000;

    /**
     * Enable retry for failed events
     */
    private boolean retryEnabled = true;

    /**
     * Maximum retry attempts
     */
    private int maxRetryAttempts = 3;

    /**
     * Retry delay
     */
    private Duration retryDelay = Duration.ofSeconds(10);

    /**
     * Database-specific configuration
     */
    private DatabaseProperties database = new DatabaseProperties();

    /**
     * Kafka-specific configuration
     */
    private KafkaProperties kafka = new KafkaProperties();

    /**
     * Additional properties
     */
    private Map<String, Object> properties;

    /**
     * Convert to EventConsumerConfiguration
     */
    public EventConsumerConfiguration toConfiguration() {
        EventConsumerConfiguration.EventSourceType sourceType = 
                "kafka".equalsIgnoreCase(source) ? 
                EventConsumerConfiguration.EventSourceType.KAFKA : 
                EventConsumerConfiguration.EventSourceType.DATABASE;

        return EventConsumerConfiguration.builder()
                .sourceType(sourceType)
                .pollingInterval(pollingInterval)
                .batchSize(batchSize)
                .bufferSize(bufferSize)
                .retryEnabled(retryEnabled)
                .maxRetryAttempts(maxRetryAttempts)
                .retryDelay(retryDelay)
                .database(database.toDatabaseConfiguration())
                .kafka(kafka.toKafkaConfiguration())
                .properties(properties)
                .build();
    }

    @Data
    public static class DatabaseProperties {
        private String tableName = "api_events";
        private String idColumn = "id";
        private String timestampColumn = "created_at";
        private String processedColumn = "processed";
        private String retryCountColumn = "retry_count";
        private String whereClause = "processed = false";
        private String orderByClause = "created_at ASC";
        private boolean reactive = true;

        public EventConsumerConfiguration.DatabaseConfiguration toDatabaseConfiguration() {
            return EventConsumerConfiguration.DatabaseConfiguration.builder()
                    .tableName(tableName)
                    .idColumn(idColumn)
                    .timestampColumn(timestampColumn)
                    .processedColumn(processedColumn)
                    .retryCountColumn(retryCountColumn)
                    .whereClause(whereClause)
                    .orderByClause(orderByClause)
                    .reactive(reactive)
                    .build();
        }
    }

    @Data
    public static class KafkaProperties {
        private String topic = "api-events";
        private String groupId = "rest-client-consumer";
        private String bootstrapServers = "localhost:9092";
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = false;
        private Duration sessionTimeout = Duration.ofSeconds(30);
        private Duration pollTimeout = Duration.ofSeconds(1);
        private boolean reactive = true;
        private Map<String, Object> additionalProperties;

        public EventConsumerConfiguration.KafkaConfiguration toKafkaConfiguration() {
            return EventConsumerConfiguration.KafkaConfiguration.builder()
                    .topic(topic)
                    .groupId(groupId)
                    .bootstrapServers(bootstrapServers)
                    .autoOffsetReset(autoOffsetReset)
                    .enableAutoCommit(enableAutoCommit)
                    .sessionTimeout(sessionTimeout)
                    .pollTimeout(pollTimeout)
                    .reactive(reactive)
                    .additionalProperties(additionalProperties)
                    .build();
        }
    }
}