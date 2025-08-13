package br.com.openfinance.infrastructure.persistence.repository;

import br.com.openfinance.application.port.output.ProcessingQueueRepository;
import br.com.openfinance.domain.processing.JobStatus;
import br.com.openfinance.domain.processing.ProcessingJob;
import br.com.openfinance.infrastructure.persistence.entity.ProcessingJobEntity;
import br.com.openfinance.infrastructure.persistence.mapper.ProcessingJobMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;

/**
 * High-performance repository using Virtual Threads and Structured Concurrency for processing queue operations.
 */
@Slf4j
@Repository("virtualThreadProcessingQueueRepository")
public class VirtualThreadProcessingQueueRepository implements ProcessingQueueRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Qualifier("virtualThreadDataSource")
    private final DataSource virtualThreadDataSource;
    
    private final ProcessingJobMapper mapper;
    private final Executor virtualThreadExecutor;
    private final MeterRegistry meterRegistry;
    private final String nodeId;
    
    // Metrics
    private final Counter jobsFetchedCounter;
    private final Counter jobsUpdatedCounter;
    private final Timer fetchBatchTimer;
    private final Timer batchProcessingTimer;
    
    public VirtualThreadProcessingQueueRepository(
            @Qualifier("virtualThreadDataSource") DataSource virtualThreadDataSource,
            ProcessingJobMapper mapper,
            MeterRegistry meterRegistry) {
        
        this.virtualThreadDataSource = virtualThreadDataSource;
        this.mapper = mapper;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.meterRegistry = meterRegistry;
        this.nodeId = generateNodeId();
        
        // Initialize metrics
        this.jobsFetchedCounter = Counter.builder("processing.queue.jobs.fetched")
                .description("Number of jobs fetched from queue")
                .register(meterRegistry);
        this.jobsUpdatedCounter = Counter.builder("processing.queue.jobs.updated")
                .description("Number of jobs updated in queue")
                .register(meterRegistry);
        this.fetchBatchTimer = Timer.builder("processing.queue.fetch.batch.time")
                .description("Time taken to fetch job batches")
                .register(meterRegistry);
        this.batchProcessingTimer = Timer.builder("processing.queue.batch.processing.time")
                .description("Time taken for batch processing operations")
                .register(meterRegistry);
    }
    
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public ProcessingJob save(ProcessingJob job) {
        ProcessingJobEntity entity = mapper.toEntity(job);
        entity = entityManager.merge(entity);
        return mapper.toDomain(entity);
    }
    
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public List<ProcessingJob> fetchNextBatch(int batchSize) {
        return Timer.Sample.start(meterRegistry)
                .stop(fetchBatchTimer)
                .wrap(() -> fetchNextBatchInternal(batchSize));
    }
    
    /**
     * High-performance batch fetching using PostgreSQL's SELECT FOR UPDATE SKIP LOCKED.
     * Optimized for Virtual Threads with direct JDBC access.
     */
    private List<ProcessingJob> fetchNextBatchInternal(int batchSize) {
        String sql = """
            WITH next_jobs AS (
                SELECT id
                FROM processing_jobs
                WHERE status = 'PENDING'
                  AND (execution_node IS NULL OR execution_node != ?)
                ORDER BY priority DESC, created_at ASC
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            UPDATE processing_jobs
            SET status = 'PROCESSING',
                execution_node = ?,
                updated_at = ?
            WHERE id IN (SELECT id FROM next_jobs)
            RETURNING id, consent_id, organization_id, status, retry_count, 
                      created_at, updated_at, error_details, execution_node,
                      priority, estimated_duration_ms, actual_duration_ms, version
            """;
        
        try (Connection connection = virtualThreadDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            connection.setAutoCommit(false);
            
            LocalDateTime now = LocalDateTime.now();
            stmt.setString(1, nodeId);
            stmt.setInt(2, batchSize);
            stmt.setString(3, nodeId);
            stmt.setObject(4, now);
            
            List<ProcessingJob> jobs = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ProcessingJob job = mapResultSetToDomain(rs);
                jobs.add(job);
            }
            
            connection.commit();
            jobsFetchedCounter.increment(jobs.size());
            
            log.debug("Fetched {} jobs for processing on node {}", jobs.size(), nodeId);
            return jobs;
            
        } catch (SQLException e) {
            log.error("Error fetching job batch: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch job batch", e);
        }
    }
    
    /**
     * Parallel batch update using Virtual Threads and Structured Concurrency.
     */
    public CompletableFuture<Void> updateJobsParallel(List<ProcessingJob> jobs) {
        return CompletableFuture.runAsync(() -> {
            Timer.Sample sample = Timer.Sample.start(meterRegistry);
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Fork update tasks for each job
                List<StructuredTaskScope.Subtask<Void>> updateTasks = jobs.stream()
                        .map(job -> scope.fork(() -> {
                            updateJobStatus(job);
                            return null;
                        }))
                        .toList();
                
                // Wait for all updates to complete
                scope.join();
                scope.throwIfFailed();
                
                jobsUpdatedCounter.increment(jobs.size());
                log.debug("Updated {} jobs in parallel", jobs.size());
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Parallel job updates interrupted", e);
            } finally {
                sample.stop(batchProcessingTimer);
            }
        }, virtualThreadExecutor);
    }
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(Long jobId, JobStatus status) {
        ProcessingJobEntity entity = entityManager.find(ProcessingJobEntity.class, jobId);
        if (entity != null) {
            entity.setStatus(status);
            entity.setUpdatedAt(LocalDateTime.now());
            entityManager.merge(entity);
            jobsUpdatedCounter.increment();
        }
    }
    
    private void updateJobStatus(ProcessingJob job) {
        String sql = """
            UPDATE processing_jobs 
            SET status = ?, updated_at = ?, error_details = ?, 
                actual_duration_ms = ?, retry_count = ?
            WHERE id = ? AND version = ?
            """;
        
        try (Connection connection = virtualThreadDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            stmt.setString(1, job.getStatus().name());
            stmt.setObject(2, LocalDateTime.now());
            stmt.setString(3, job.getErrorDetails());
            stmt.setObject(4, null); // Will be calculated by domain
            stmt.setInt(5, job.getRetryCount());
            stmt.setLong(6, job.getId());
            stmt.setLong(7, 0); // Version handling
            
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                log.warn("Optimistic lock failure updating job {}", job.getId());
            }
            
        } catch (SQLException e) {
            log.error("Error updating job {}: {}", job.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to update job", e);
        }
    }
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void moveToDeadLetter(Long jobId) {
        String sql = """
            UPDATE processing_jobs
            SET status = 'DEAD_LETTER',
                updated_at = ?,
                execution_node = ?
            WHERE id = ?
            """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, LocalDateTime.now());
        query.setParameter(2, nodeId);
        query.setParameter(3, jobId);
        
        int updated = query.executeUpdate();
        if (updated > 0) {
            jobsUpdatedCounter.increment();
            log.info("Moved job {} to dead letter queue", jobId);
        }
    }
    
    /**
     * Batch processing method using Structured Concurrency for maximum throughput.
     */
    public List<ProcessingJob> processBatchWithStructuredConcurrency(
            List<ProcessingJob> jobs, 
            java.util.function.Function<ProcessingJob, ProcessingJob> processor) 
            throws InterruptedException {
        
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Fork processing tasks
            List<StructuredTaskScope.Subtask<ProcessingJob>> tasks = jobs.stream()
                    .map(job -> scope.fork(() -> {
                        try {
                            return processor.apply(job);
                        } catch (Exception e) {
                            log.error("Error processing job {}: {}", job.getId(), e.getMessage());
                            throw e;
                        }
                    }))
                    .toList();
            
            // Wait for all tasks to complete
            scope.join();
            scope.throwIfFailed();
            
            // Collect results
            List<ProcessingJob> results = tasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();
            
            log.debug("Processed batch of {} jobs using Structured Concurrency", results.size());
            return results;
            
        } finally {
            sample.stop(batchProcessingTimer);
        }
    }
    
    /**
     * Get queue statistics for monitoring and capacity planning.
     */
    public QueueStatistics getQueueStatistics() {
        String sql = """
            SELECT 
                status,
                COUNT(*) as count,
                AVG(actual_duration_ms) as avg_duration_ms,
                MIN(created_at) as oldest_job
            FROM processing_jobs 
            WHERE created_at > NOW() - INTERVAL '24 hours'
            GROUP BY status
            """;
        
        try (Connection connection = virtualThreadDataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            QueueStatistics stats = new QueueStatistics();
            
            while (rs.next()) {
                String status = rs.getString("status");
                long count = rs.getLong("count");
                Double avgDuration = rs.getObject("avg_duration_ms", Double.class);
                LocalDateTime oldestJob = rs.getObject("oldest_job", LocalDateTime.class);
                
                stats.addStatusCount(status, count, avgDuration, oldestJob);
            }
            
            return stats;
            
        } catch (SQLException e) {
            log.error("Error getting queue statistics: {}", e.getMessage(), e);
            return new QueueStatistics();
        }
    }
    
    private ProcessingJob mapResultSetToDomain(ResultSet rs) throws SQLException {
        return ProcessingJob.builder()
                .id(rs.getLong("id"))
                .consentId(rs.getObject("consent_id", java.util.UUID.class))
                .organizationId(rs.getString("organization_id"))
                .status(JobStatus.valueOf(rs.getString("status")))
                .retryCount(rs.getInt("retry_count"))
                .createdAt(rs.getObject("created_at", LocalDateTime.class))
                .updatedAt(rs.getObject("updated_at", LocalDateTime.class))
                .errorDetails(rs.getString("error_details"))
                .build();
    }
    
    private String generateNodeId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + 
                   Thread.currentThread().threadId();
        } catch (UnknownHostException e) {
            return "unknown-" + System.currentTimeMillis();
        }
    }
    
    // Statistics record for monitoring
    public static class QueueStatistics {
        private final java.util.Map<String, StatusStats> statusStats = new java.util.HashMap<>();
        
        public void addStatusCount(String status, long count, Double avgDuration, LocalDateTime oldestJob) {
            statusStats.put(status, new StatusStats(count, avgDuration, oldestJob));
        }
        
        public java.util.Map<String, StatusStats> getStatusStats() {
            return statusStats;
        }
        
        public record StatusStats(long count, Double avgDurationMs, LocalDateTime oldestJob) {}
    }
}