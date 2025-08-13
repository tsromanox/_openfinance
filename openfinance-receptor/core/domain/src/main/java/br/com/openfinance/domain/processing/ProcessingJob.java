package br.com.openfinance.domain.processing;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ProcessingJob {
    private static final int MAX_RETRY_COUNT = 3;
    
    private final Long id;
    private final UUID consentId;
    private final String organizationId;
    private final JobStatus status;
    private final Integer retryCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String errorDetails;

    private ProcessingJob(Builder builder) {
        this.id = builder.id;
        this.consentId = Objects.requireNonNull(builder.consentId, "ConsentId cannot be null");
        this.organizationId = Objects.requireNonNull(builder.organizationId, "OrganizationId cannot be null");
        this.status = Objects.requireNonNull(builder.status, "Status cannot be null");
        this.retryCount = builder.retryCount != null ? builder.retryCount : 0;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "CreatedAt cannot be null");
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : LocalDateTime.now();
        this.errorDetails = builder.errorDetails;
        
        validateBusinessRules();
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Long getId() { return id; }
    public UUID getConsentId() { return consentId; }
    public String getOrganizationId() { return organizationId; }
    public JobStatus getStatus() { return status; }
    public Integer getRetryCount() { return retryCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getErrorDetails() { return errorDetails; }

    // Business logic methods
    public boolean canRetry() {
        return status == JobStatus.FAILED && retryCount < MAX_RETRY_COUNT;
    }

    public boolean shouldMoveToDeadLetter() {
        return status == JobStatus.FAILED && retryCount >= MAX_RETRY_COUNT;
    }

    public ProcessingJob withStatus(JobStatus newStatus) {
        return builder()
                .id(this.id)
                .consentId(this.consentId)
                .organizationId(this.organizationId)
                .status(newStatus)
                .retryCount(this.retryCount)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .errorDetails(this.errorDetails)
                .build();
    }

    public ProcessingJob withError(String errorMessage) {
        int newRetryCount = this.retryCount + 1;
        JobStatus newStatus = newRetryCount >= MAX_RETRY_COUNT ? JobStatus.DEAD_LETTER : JobStatus.FAILED;
        
        return builder()
                .id(this.id)
                .consentId(this.consentId)
                .organizationId(this.organizationId)
                .status(newStatus)
                .retryCount(newRetryCount)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .errorDetails(errorMessage)
                .build();
    }

    public boolean isExpired() {
        // Job expira após 24 horas sem processamento
        return createdAt.isBefore(LocalDateTime.now().minusHours(24)) && 
               status == JobStatus.PENDING;
    }

    private void validateBusinessRules() {
        if (organizationId.trim().isEmpty()) {
            throw new IllegalArgumentException("OrganizationId cannot be empty");
        }
        if (retryCount < 0) {
            throw new IllegalArgumentException("RetryCount cannot be negative");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("UpdatedAt cannot be before CreatedAt");
        }
    }

    public static class Builder {
        private Long id;
        private UUID consentId;
        private String organizationId;
        private JobStatus status;
        private Integer retryCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String errorDetails;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder consentId(UUID consentId) {
            this.consentId = consentId;
            return this;
        }

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Builder status(JobStatus status) {
            this.status = status;
            return this;
        }

        public Builder retryCount(Integer retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder errorDetails(String errorDetails) {
            this.errorDetails = errorDetails;
            return this;
        }

        public ProcessingJob build() {
            return new ProcessingJob(this);
        }
    }
}
