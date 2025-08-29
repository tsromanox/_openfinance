package com.bank.openfinance.client.event.source.database;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing an event in the database.
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_status_process_after", columnList = "status, process_after"),
        @Index(name = "idx_correlation_id", columnList = "correlation_id"),
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_priority_created", columnList = "priority, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "process_after")
    private Instant processAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "last_error_at")
    private Instant lastErrorAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb")
    private Map<String, Object> headers;

    @Column(name = "priority", length = 20)
    @Builder.Default
    private String priority = "NORMAL";

    @Version
    @Column(name = "version")
    private Long version;

    public enum EventStatus {
        PENDING,      // Waiting to be processed
        PROCESSING,   // Currently being processed
        PROCESSED,    // Successfully processed
        RETRY,        // Scheduled for retry
        FAILED,       // Failed after max retries
        CANCELLED,    // Manually cancelled
        DEAD_LETTER   // Sent to DLQ
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = EventStatus.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (priority == null) {
            priority = "NORMAL";
        }
    }
}