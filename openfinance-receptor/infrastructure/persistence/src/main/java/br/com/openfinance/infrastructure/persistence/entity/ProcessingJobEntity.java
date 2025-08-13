package br.com.openfinance.infrastructure.persistence.entity;

import br.com.openfinance.domain.processing.JobStatus;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Optimized JPA entity for ProcessingJob with batch processing support and performance indexes.
 */
@Entity
@Table(name = "processing_jobs", indexes = {
    @Index(name = "idx_processing_jobs_status_created", columnList = "status, created_at"),
    @Index(name = "idx_processing_jobs_consent_org", columnList = "consent_id, organization_id"),
    @Index(name = "idx_processing_jobs_status_retry", columnList = "status, retry_count"),
    @Index(name = "idx_processing_jobs_updated_at", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@DynamicUpdate
@OptimisticLocking(type = OptimisticLockType.VERSION)
@SelectBeforeUpdate(false) // Performance optimization
public class ProcessingJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "job_seq")
    @SequenceGenerator(name = "job_seq", sequenceName = "job_seq", allocationSize = 100)
    private Long id;

    @Column(name = "consent_id", nullable = false)
    private UUID consentId;

    @Column(name = "organization_id", nullable = false, length = 100)
    private String organizationId;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;
    
    @Column(name = "execution_node", length = 50)
    private String executionNode; // For distributed processing
    
    @Column(name = "priority")
    private Integer priority = 0; // Job priority for queue ordering
    
    @Column(name = "estimated_duration_ms")
    private Long estimatedDurationMs; // For capacity planning
    
    @Column(name = "actual_duration_ms")
    private Long actualDurationMs; // For performance monitoring

    @Version
    private Long version;

    // Helper methods for domain logic
    public boolean canRetry(int maxRetries) {
        return status == JobStatus.FAILED && retryCount < maxRetries;
    }
    
    public boolean shouldMoveToDeadLetter(int maxRetries) {
        return status == JobStatus.FAILED && retryCount >= maxRetries;
    }
    
    public boolean isExpired(int hoursThreshold) {
        return createdAt.isBefore(LocalDateTime.now().minusHours(hoursThreshold)) && 
               status == JobStatus.PENDING;
    }
    
    public void incrementRetryCount() {
        this.retryCount = this.retryCount != null ? this.retryCount + 1 : 1;
    }
    
    public void markStarted(String nodeName) {
        this.status = JobStatus.PROCESSING;
        this.executionNode = nodeName;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markCompleted(long durationMs) {
        this.status = JobStatus.COMPLETED;
        this.actualDurationMs = durationMs;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markFailed(String errorMessage, long durationMs) {
        this.status = JobStatus.FAILED;
        this.errorDetails = errorMessage;
        this.actualDurationMs = durationMs;
        incrementRetryCount();
        this.updatedAt = LocalDateTime.now();
    }
}
