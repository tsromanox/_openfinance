package com.bank.openfinance.client.event.source.database;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for event entities.
 */
@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    /**
     * Find unprocessed events ready for processing.
     */
    @Query("""
        SELECT e FROM EventEntity e 
        WHERE e.status IN :statuses 
        AND (e.processAfter IS NULL OR e.processAfter <= :now)
        ORDER BY e.priority ASC, e.createdAt ASC
        """)
    List<EventEntity> findUnprocessedEvents(
            @Param("statuses") List<EventEntity.EventStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable
    );

    /**
     * Find events by status with pagination.
     */
    List<EventEntity> findByStatusOrderByCreatedAtAsc(
            EventEntity.EventStatus status,
            Pageable pageable
    );

    /**
     * Count events by status.
     */
    long countByStatus(EventEntity.EventStatus status);

    /**
     * Count events by multiple statuses.
     */
    long countByStatusIn(List<EventEntity.EventStatus> statuses);

    /**
     * Update status for multiple events.
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE EventEntity e 
        SET e.status = :status, e.processedAt = :processedAt 
        WHERE e.id IN :ids
        """)
    int updateStatusByIds(
            @Param("ids") List<UUID> ids,
            @Param("status") EventEntity.EventStatus status,
            @Param("processedAt") Instant processedAt
    );

    /**
     * Find events for retry.
     */
    @Query("""
        SELECT e FROM EventEntity e 
        WHERE e.status = 'RETRY' 
        AND e.retryCount < :maxRetries 
        AND e.processAfter <= :now
        ORDER BY e.processAfter ASC
        """)
    List<EventEntity> findEventsForRetry(
            @Param("maxRetries") int maxRetries,
            @Param("now") Instant now,
            Pageable pageable
    );

    /**
     * Clean up old processed events.
     */
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM EventEntity e 
        WHERE e.status = 'PROCESSED' 
        AND e.processedAt < :cutoffDate
        """)
    int deleteOldProcessedEvents(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Find events by correlation ID.
     */
    List<EventEntity> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);

    /**
     * Find events by tenant ID and status.
     */
    List<EventEntity> findByTenantIdAndStatusOrderByCreatedAtAsc(
            String tenantId,
            EventEntity.EventStatus status,
            Pageable pageable
    );

    /**
     * Get event statistics.
     */
    @Query("""
        SELECT e.status as status, COUNT(e) as count 
        FROM EventEntity e 
        GROUP BY e.status
        """)
    List<EventStatusCount> getEventStatistics();

    /**
     * Interface for event status counts.
     */
    interface EventStatusCount {
        EventEntity.EventStatus getStatus();
        Long getCount();
    }
}
