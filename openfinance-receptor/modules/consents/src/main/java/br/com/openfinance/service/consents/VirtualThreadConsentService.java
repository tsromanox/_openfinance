package br.com.openfinance.service.consents;

import br.com.openfinance.application.port.input.ConsentUseCase;
import br.com.openfinance.application.port.output.ConsentRepository;
import br.com.openfinance.application.port.output.OpenFinanceClient;
import br.com.openfinance.application.port.output.ProcessingQueueRepository;
import br.com.openfinance.domain.consent.Consent;
import br.com.openfinance.domain.consent.ConsentStatus;
import br.com.openfinance.service.consents.monitoring.ConsentPerformanceMonitor;
import br.com.openfinance.service.consents.processor.VirtualThreadConsentProcessor;
import br.com.openfinance.service.consents.resource.AdaptiveConsentResourceManager;
import br.com.openfinance.service.consents.validation.ParallelConsentValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced consent service that leverages Virtual Threads, Structured Concurrency,
 * and adaptive resource management for maximum performance in consent operations.
 * 
 * This service extends the existing ConsentUseCase interface while providing
 * advanced parallel processing capabilities.
 */
@Slf4j
@Service
public class VirtualThreadConsentService implements ConsentUseCase {

    private final ConsentRepository consentRepository;
    private final ProcessingQueueRepository queueRepository;
    private final OpenFinanceClient openFinanceClient;
    private final VirtualThreadConsentProcessor consentProcessor;
    private final ParallelConsentValidator consentValidator;
    private final AdaptiveConsentResourceManager resourceManager;
    private final ConsentPerformanceMonitor performanceMonitor;
    private final TaskExecutor virtualThreadExecutor;

    public VirtualThreadConsentService(
            ConsentRepository consentRepository,
            ProcessingQueueRepository queueRepository,
            OpenFinanceClient openFinanceClient,
            VirtualThreadConsentProcessor consentProcessor,
            ParallelConsentValidator consentValidator,
            AdaptiveConsentResourceManager resourceManager,
            ConsentPerformanceMonitor performanceMonitor,
            @Qualifier("consentVirtualThreadExecutor") TaskExecutor virtualThreadExecutor) {

        this.consentRepository = consentRepository;
        this.queueRepository = queueRepository;
        this.openFinanceClient = openFinanceClient;
        this.consentProcessor = consentProcessor;
        this.consentValidator = consentValidator;
        this.resourceManager = resourceManager;
        this.performanceMonitor = performanceMonitor;
        this.virtualThreadExecutor = virtualThreadExecutor;

        log.info("VirtualThreadConsentService initialized with advanced parallel processing capabilities");
    }

    @Override
    public Consent createConsent(CreateConsentCommand command) {
        try {
            // Use the enhanced processor for parallel validation and creation
            CompletableFuture<Consent> consentFuture = consentProcessor.createConsentAsync(command);
            
            // Block and return the result (maintaining interface compatibility)
            return consentFuture.get();
            
        } catch (Exception e) {
            log.error("Error creating consent for organization {}: {}", 
                    command.organizationId(), e.getMessage(), e);
            performanceMonitor.recordError("creation_error", "CREATE_CONSENT", false);
            throw new RuntimeException("Failed to create consent", e);
        }
    }

    @Override
    public Consent getConsent(UUID consentId) {
        try {
            return consentRepository.findById(consentId)
                    .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + consentId));
        } catch (Exception e) {
            log.error("Error retrieving consent {}: {}", consentId, e.getMessage(), e);
            performanceMonitor.recordError("retrieval_error", "GET_CONSENT", false);
            throw e;
        }
    }

    @Override
    public void processConsent(UUID consentId) {
        try {
            // Use the enhanced processor for parallel processing
            List<UUID> consentIds = List.of(consentId);
            consentProcessor.processConsentBatchWithStructuredConcurrency(consentIds);
            
        } catch (Exception e) {
            log.error("Error processing consent {}: {}", consentId, e.getMessage(), e);
            performanceMonitor.recordError("processing_error", "PROCESS_CONSENT", true);
            throw new RuntimeException("Failed to process consent", e);
        }
    }

    @Override
    public void revokeConsent(UUID consentId) {
        try {
            // Use the enhanced processor for parallel revocation
            CompletableFuture<Void> revocationFuture = consentProcessor.revokeConsentsAsync(
                    List.of(consentId), "Manual revocation");
            
            // Block and wait for completion (maintaining interface compatibility)
            revocationFuture.get();
            
        } catch (Exception e) {
            log.error("Error revoking consent {}: {}", consentId, e.getMessage(), e);
            performanceMonitor.recordError("revocation_error", "REVOKE_CONSENT", false);
            throw new RuntimeException("Failed to revoke consent", e);
        }
    }

    // Enhanced methods using Virtual Threads and Structured Concurrency

    /**
     * Create multiple consents concurrently using Virtual Threads.
     */
    @Async("consentVirtualThreadExecutor")
    public CompletableFuture<List<Consent>> createConsentsAsync(List<CreateConsentCommand> commands) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating {} consents concurrently using Virtual Threads", commands.size());
                
                List<CompletableFuture<Consent>> consentFutures = commands.stream()
                        .map(consentProcessor::createConsentAsync)
                        .toList();
                
                // Wait for all consents to be created
                CompletableFuture<Void> allOf = CompletableFuture.allOf(
                        consentFutures.toArray(new CompletableFuture[0]));
                
                allOf.get();
                
                // Collect results
                List<Consent> createdConsents = consentFutures.stream()
                        .map(CompletableFuture::join)
                        .toList();
                
                log.info("Successfully created {} consents concurrently", createdConsents.size());
                return createdConsents;
                
            } catch (Exception e) {
                log.error("Error creating consents concurrently", e);
                performanceMonitor.recordError("batch_creation_error", "CREATE_CONSENTS_BATCH", true);
                throw new RuntimeException("Failed to create consents", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Process multiple consents using Structured Concurrency for optimal resource management.
     */
    @Async("consentVirtualThreadExecutor")
    public CompletableFuture<BatchProcessingResult> processConsentsWithStructuredConcurrency(
            List<UUID> consentIds) {
        
        return consentProcessor.processConsentBatchWithStructuredConcurrency(consentIds)
                .thenApply(result -> new BatchProcessingResult(
                        result.processed(),
                        result.successful(),
                        result.failed(),
                        result.durationMs(),
                        "STRUCTURED_CONCURRENCY"
                ));
    }

    /**
     * Validate consent permissions using parallel processing.
     */
    @Async("consentValidationExecutor")
    public CompletableFuture<ConsentValidationSummary> validateConsentsAsync(
            List<UUID> consentIds, Set<String> requiredPermissions) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Validating {} consents with parallel processing", consentIds.size());
                
                List<CompletableFuture<VirtualThreadConsentProcessor.ConsentValidationResult>> validationFutures = 
                        consentIds.stream()
                        .map(consentId -> consentProcessor.validateConsentPermissionsAsync(consentId, requiredPermissions))
                        .toList();
                
                // Wait for all validations to complete
                CompletableFuture.allOf(validationFutures.toArray(new CompletableFuture[0])).get();
                
                // Collect results
                List<VirtualThreadConsentProcessor.ConsentValidationResult> validationResults = 
                        validationFutures.stream()
                        .map(CompletableFuture::join)
                        .toList();
                
                long validCount = validationResults.stream()
                        .mapToLong(result -> result.valid() ? 1 : 0)
                        .sum();
                
                long invalidCount = validationResults.size() - validCount;
                
                ConsentValidationSummary summary = new ConsentValidationSummary(
                        consentIds.size(),
                        (int) validCount,
                        (int) invalidCount,
                        validationResults
                );
                
                log.debug("Consent validation completed: {} valid, {} invalid", validCount, invalidCount);
                return summary;
                
            } catch (Exception e) {
                log.error("Error validating consents", e);
                performanceMonitor.recordError("validation_error", "VALIDATE_CONSENTS_BATCH", true);
                throw new RuntimeException("Failed to validate consents", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Process pending consents using adaptive resource management.
     */
    @Async("consentVirtualThreadExecutor")
    public CompletableFuture<Integer> processPendingConsentsAdaptively() {
        return consentProcessor.processPendingConsentsWithVirtualThreads();
    }

    /**
     * Reactive consent processing using Project Reactor with Virtual Thread integration.
     */
    public Flux<ConsentProcessingResult> processConsentsReactively(List<UUID> consentIds) {
        return consentProcessor.processConsentsReactive(consentIds)
                .map(result -> new ConsentProcessingResult(
                        result.consentId(),
                        result.success(),
                        result.errorMessage(),
                        result.newStatus()
                ));
    }

    /**
     * Revoke multiple consents concurrently.
     */
    @Async("consentVirtualThreadExecutor")
    public CompletableFuture<Void> revokeConsentsAsync(List<UUID> consentIds, String revocationReason) {
        return consentProcessor.revokeConsentsAsync(consentIds, revocationReason);
    }

    /**
     * Get consents by status using reactive processing.
     */
    public Flux<Consent> getConsentsByStatusReactively(ConsentStatus status, int limit) {
        return Flux.fromIterable(consentRepository.findByStatusWithLimit(status, limit))
                .doOnNext(consent -> log.trace("Retrieved consent: {}", consent.getId()))
                .doOnError(error -> {
                    log.error("Error retrieving consents by status {}: {}", status, error.getMessage(), error);
                    performanceMonitor.recordError("query_error", "GET_CONSENTS_BY_STATUS", false);
                });
    }

    /**
     * Process consents by organization with resource management.
     */
    @Async("consentVirtualThreadExecutor")
    public CompletableFuture<OrganizationProcessingResult> processConsentsByOrganization(
            String organizationId, int batchSize) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Acquire resources before processing
                if (!resourceManager.acquireConsentProcessingResources()) {
                    throw new RuntimeException("Unable to acquire processing resources");
                }
                
                try {
                    List<Consent> consents = consentRepository.findByOrganizationIdWithLimit(
                            organizationId, batchSize);
                    
                    if (consents.isEmpty()) {
                        return new OrganizationProcessingResult(organizationId, 0, 0, 0);
                    }
                    
                    List<UUID> consentIds = consents.stream()
                            .map(Consent::getId)
                            .toList();
                    
                    var batchResult = consentProcessor.processConsentBatchWithStructuredConcurrency(consentIds).get();
                    
                    return new OrganizationProcessingResult(
                            organizationId,
                            batchResult.processed(),
                            batchResult.successful(),
                            batchResult.failed()
                    );
                    
                } finally {
                    resourceManager.releaseConsentProcessingResources();
                }
                
            } catch (Exception e) {
                log.error("Error processing consents for organization {}: {}", organizationId, e.getMessage(), e);
                performanceMonitor.recordError("organization_processing_error", "PROCESS_BY_ORGANIZATION", true);
                throw new RuntimeException("Failed to process consents for organization", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Validate consent business rules using parallel validation.
     */
    @Async("consentValidationExecutor")
    public CompletableFuture<ConsentBusinessValidationResult> validateConsentBusinessRules(UUID consentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!resourceManager.acquireValidationResources()) {
                    throw new RuntimeException("Unable to acquire validation resources");
                }
                
                try {
                    return consentValidator.validateBusinessRules(consentId);
                } finally {
                    resourceManager.releaseValidationResources();
                }
                
            } catch (Exception e) {
                log.error("Error validating business rules for consent {}: {}", consentId, e.getMessage(), e);
                performanceMonitor.recordError("business_validation_error", "VALIDATE_BUSINESS_RULES", true);
                throw new RuntimeException("Failed to validate business rules", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Get comprehensive consent statistics using parallel aggregation.
     */
    @Async("consentVirtualThreadExecutor")
    public CompletableFuture<ConsentStatistics> getConsentStatisticsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Parallel aggregation of consent statistics
                var totalConsentsTask = scope.fork(() -> consentRepository.countAll());
                var activeConsentsTask = scope.fork(() -> consentRepository.countByStatus(ConsentStatus.AUTHORISED));
                var pendingConsentsTask = scope.fork(() -> consentRepository.countByStatus(ConsentStatus.AWAITING_AUTHORISATION));
                var revokedConsentsTask = scope.fork(() -> consentRepository.countByStatus(ConsentStatus.REJECTED));
                var expiredConsentsTask = scope.fork(() -> consentRepository.countByStatus(ConsentStatus.EXPIRED));
                
                scope.join();
                scope.throwIfFailed();
                
                return new ConsentStatistics(
                        totalConsentsTask.get(),
                        activeConsentsTask.get(),
                        pendingConsentsTask.get(),
                        revokedConsentsTask.get(),
                        expiredConsentsTask.get()
                );
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Statistics collection interrupted", e);
            } catch (Exception e) {
                log.error("Error collecting consent statistics", e);
                performanceMonitor.recordError("statistics_error", "GET_STATISTICS", false);
                throw new RuntimeException("Failed to collect statistics", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Monitor and get current processing status.
     */
    public ConsentServiceStatus getServiceStatus() {
        var resourceUtilization = resourceManager.getResourceUtilization();
        var performanceReport = performanceMonitor.getPerformanceReport();
        var recommendations = performanceMonitor.getRecommendations();
        
        return new ConsentServiceStatus(
                true, // Service is running
                resourceUtilization.activeProcessingTasks(),
                resourceUtilization.activeValidationTasks(),
                resourceUtilization.activeApiCalls(),
                resourceUtilization.getProcessingUtilizationPercentage(),
                resourceUtilization.getValidationUtilizationPercentage(),
                resourceUtilization.getApiCallUtilizationPercentage(),
                performanceReport.currentThroughput(),
                performanceReport.processingEfficiency(),
                performanceReport.errorRate(),
                resourceUtilization.isUnderPressure(),
                recommendations
        );
    }

    // Result records for enhanced operations

    public record BatchProcessingResult(
            int processed,
            int successful,
            int failed,
            long durationMs,
            String strategy
    ) {}

    public record ConsentValidationSummary(
            int totalValidated,
            int validCount,
            int invalidCount,
            List<VirtualThreadConsentProcessor.ConsentValidationResult> results
    ) {}

    public record ConsentProcessingResult(
            UUID consentId,
            boolean success,
            String errorMessage,
            ConsentStatus newStatus
    ) {}

    public record OrganizationProcessingResult(
            String organizationId,
            int processed,
            int successful,
            int failed
    ) {}

    public record ConsentBusinessValidationResult(
            UUID consentId,
            boolean valid,
            List<String> validationErrors,
            List<String> warnings
    ) {}

    public record ConsentStatistics(
            long totalConsents,
            long activeConsents,
            long pendingConsents,
            long revokedConsents,
            long expiredConsents
    ) {}

    public record ConsentServiceStatus(
            boolean isRunning,
            int activeProcessingTasks,
            int activeValidationTasks,
            int activeApiCalls,
            double processingUtilizationPercentage,
            double validationUtilizationPercentage,
            double apiCallUtilizationPercentage,
            double currentThroughput,
            double processingEfficiency,
            double errorRate,
            boolean isUnderPressure,
            ConsentPerformanceMonitor.ConsentPerformanceRecommendations recommendations
    ) {}
}