package com.bank.openfinance.client.event.source.database;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for database event source.
 */
@Data
@Configuration
@Validated
@ConfigurationProperties(prefix = "openfinance.database.events")
public class DatabaseEventConfiguration {

    /**
     * Enable virtual threads for processing
     */
    private boolean useVirtualThreads = true;

    /**
     * Thread pool size (when not using virtual threads)
     */
    @Min(1)
    private int threadPoolSize = 10;

    /**
     * Polling interval in milliseconds
     */
    @Positive
    private long pollInterval = 5000;

    /**
     * Poll timeout in milliseconds
     */
    @Positive
    private long pollTimeout = 1000;

    /**
     * Batch size for fetching events
     */
    @Positive
    private int batchSize = 100;

    /**
     * Maximum number of retries for failed events
     */
    @Min(0)
    private int maxRetries = 3;

    /**
     * Queue capacity for in-memory event buffer
     */
    @Positive
    private int queueCapacity = 10000;

    /**
     * Maximum pending events before health check fails
     */
    @Positive
    private long maxPendingEvents = 100000;

    /**
     * Enable database connection pooling
     */
    private boolean connectionPoolingEnabled = true;

    /**
     * Maximum database connections
     */
    @Positive
    private int maxConnections = 20;

    /**
     * Query timeout in seconds
     */
    @Positive
    private int queryTimeout = 30;

    /**
     * Enable batch updates for acknowledgments
     */
    private boolean batchAcknowledgment = true;

    /**
     * Batch acknowledgment size
     */
    @Positive
    private int acknowledgmentBatchSize = 50;
}
