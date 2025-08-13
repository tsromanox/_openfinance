package br.com.openfinance.infrastructure.persistence.repository;

import br.com.openfinance.application.port.output.ProcessingQueueRepository;
import br.com.openfinance.domain.processing.JobStatus;
import br.com.openfinance.domain.processing.ProcessingJob;
import br.com.openfinance.infrastructure.persistence.entity.ProcessingJobEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ProcessingQueueRepositoryImpl implements ProcessingQueueRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ProcessingJob save(ProcessingJob job) {
        return null;
    }

    @Override
    @Transactional
    public List<ProcessingJob> fetchNextBatch(int batchSize) {
        // SELECT FOR UPDATE SKIP LOCKED - Padrão para fila em PostgreSQL
        String sql = """
            WITH next_jobs AS (
                SELECT id
                FROM processing_jobs
                WHERE status = 'PENDING'
                ORDER BY created_at
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
            )
            UPDATE processing_jobs
            SET status = 'PROCESSING',
                updated_at = :now
            WHERE id IN (SELECT id FROM next_jobs)
            RETURNING *
            """;

        var query = entityManager.createNativeQuery(sql, ProcessingJobEntity.class)
                .setParameter("batchSize", batchSize)
                .setParameter("now", LocalDateTime.now());

        List<ProcessingJobEntity> entities = query.getResultList();

        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void updateStatus(Long jobId, JobStatus status) {
        var entity = entityManager.find(ProcessingJobEntity.class, jobId);
        if (entity != null) {
            entity.setStatus(status);
            entity.setUpdatedAt(LocalDateTime.now());
        }
    }

    @Override
    @Transactional
    public void moveToDeadLetter(Long jobId) {
        String sql = """
            UPDATE processing_jobs
            SET status = 'DEAD_LETTER',
                updated_at = :now
            WHERE id = :jobId
            """;

        entityManager.createNativeQuery(sql)
                .setParameter("jobId", jobId)
                .setParameter("now", LocalDateTime.now())
                .executeUpdate();
    }

    private ProcessingJob toDomain(ProcessingJobEntity entity) {
        return ProcessingJob.builder()
                .id(entity.getId())
                .consentId(entity.getConsentId())
                .organizationId(entity.getOrganizationId())
                .status(entity.getStatus())
                .retryCount(entity.getRetryCount())
                .createdAt(entity.getCreatedAt())
                .errorDetails(entity.getErrorDetails())
                .build();
    }
}
