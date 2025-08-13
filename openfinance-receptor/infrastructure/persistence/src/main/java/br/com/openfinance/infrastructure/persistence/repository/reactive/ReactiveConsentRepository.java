package br.com.openfinance.infrastructure.persistence.repository.reactive;

import br.com.openfinance.domain.consent.ConsentStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reactive repository for high-performance consent operations using R2DBC.
 */
public interface ReactiveConsentRepository extends R2dbcRepository<ReactiveConsentEntity, UUID> {
    
    /**
     * Find consent by external consent ID with reactive streaming.
     */
    Mono<ReactiveConsentEntity> findByConsentId(String consentId);
    
    /**
     * Find all consents for an organization with status filtering.
     */
    @Query("SELECT * FROM consents WHERE organization_id = :orgId AND status = :status ORDER BY created_at DESC")
    Flux<ReactiveConsentEntity> findByOrganizationIdAndStatus(
            @Param("orgId") String organizationId, 
            @Param("status") String status);
    
    /**
     * Find active consents that need synchronization.
     */
    @Query("""
        SELECT c.* FROM consents c 
        LEFT JOIN accounts a ON c.id = a.consent_id 
        WHERE c.status = 'AUTHORISED' 
          AND (a.last_sync_at IS NULL OR a.last_sync_at < :threshold)
        GROUP BY c.id
        ORDER BY c.created_at
        LIMIT :batchSize
        """)
    Flux<ReactiveConsentEntity> findConsentsNeedingSync(
            @Param("threshold") LocalDateTime threshold,
            @Param("batchSize") int batchSize);
    
    /**
     * Count consents by status for monitoring.
     */
    @Query("SELECT COUNT(*) FROM consents WHERE status = :status")
    Mono<Long> countByStatus(@Param("status") String status);
    
    /**
     * Find expiring consents for cleanup.
     */
    @Query("""
        SELECT * FROM consents 
        WHERE expiration_date_time < :expirationThreshold 
          AND status NOT IN ('EXPIRED', 'REJECTED')
        ORDER BY expiration_date_time
        LIMIT :batchSize
        """)
    Flux<ReactiveConsentEntity> findExpiringConsents(
            @Param("expirationThreshold") LocalDateTime expirationThreshold,
            @Param("batchSize") int batchSize);
    
    /**
     * Batch update consent statuses for performance.
     */
    @Query("""
        UPDATE consents 
        SET status = :newStatus, updated_at = :updatedAt
        WHERE id IN (:consentIds) AND status = :currentStatus
        """)
    Mono<Integer> updateStatusBatch(
            @Param("consentIds") Flux<UUID> consentIds,
            @Param("currentStatus") String currentStatus,
            @Param("newStatus") String newStatus,
            @Param("updatedAt") LocalDateTime updatedAt);
    
    /**
     * Complex query for dashboard statistics.
     */
    @Query("""
        SELECT 
            status,
            COUNT(*) as count,
            DATE_TRUNC('day', created_at) as date
        FROM consents 
        WHERE created_at >= :startDate
        GROUP BY status, DATE_TRUNC('day', created_at)
        ORDER BY date DESC, status
        """)
    Flux<ConsentStatistics> getConsentStatistics(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Performance-optimized existence check.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM consents WHERE consent_id = :consentId)")
    Mono<Boolean> existsByConsentId(@Param("consentId") String consentId);
    
    // Statistics projection for monitoring
    interface ConsentStatistics {
        String getStatus();
        Long getCount();
        LocalDateTime getDate();
    }
}