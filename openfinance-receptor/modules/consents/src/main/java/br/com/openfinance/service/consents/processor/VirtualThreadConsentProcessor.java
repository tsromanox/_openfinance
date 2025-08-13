package br.com.openfinance.service.consents.processor;

import br.com.openfinance.application.dto.ConsentRequest;
import br.com.openfinance.application.dto.ConsentResponse;
import br.com.openfinance.application.port.input.ConsentUseCase;
import br.com.openfinance.application.port.output.ConsentRepository;
import br.com.openfinance.application.port.output.OpenFinanceClient;
import br.com.openfinance.application.port.output.ProcessingQueueRepository;
import br.com.openfinance.domain.consent.Consent;
import br.com.openfinance.domain.consent.ConsentStatus;
import br.com.openfinance.domain.processing.JobStatus;
import br.com.openfinance.domain.processing.ProcessingJob;
import br.com.openfinance.service.consents.monitoring.ConsentPerformanceMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Advanced consent processor using Virtual Threads and Structured Concurrency
 * for maximum performance in consent lifecycle operations.
 */
@Slf4j
@Service
public class VirtualThreadConsentProcessor {

    private final ConsentRepository consentRepository;
    private final ProcessingQueueRepository queueRepository;
    private final OpenFinanceClient openFinanceClient;
    private final TaskExecutor virtualThreadExecutor;
    private final TaskExecutor structuredConcurrencyExecutor;
    private final TaskExecutor apiCallExecutor;
    private final MeterRegistry meterRegistry;
    private final ConsentPerformanceMonitor performanceMonitor;

    // Configuration
    @Value("${openfinance.consents.batch.size:200}")
    private int batchSize;

    @Value("${openfinance.consents.batch.max-concurrent:100}")
    private int maxConcurrentBatches;

    @Value("${openfinance.consents.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${openfinance.consents.timeout.api-call:30s}")
    private Duration apiCallTimeout;

    @Value("${openfinance.consents.timeout.batch:300s}")
    private Duration batchTimeout;

    @Value("${openfinance.consents.validation.parallel:true}")
    private boolean enableParallelValidation;

    // Performance tracking
    private final Semaphore batchSemaphore;

    public VirtualThreadConsentProcessor(
            ConsentRepository consentRepository,
            ProcessingQueueRepository queueRepository,
            OpenFinanceClient openFinanceClient,
            @Qualifier("consentVirtualThreadExecutor") TaskExecutor virtualThreadExecutor,
            @Qualifier("consentStructuredConcurrencyExecutor") TaskExecutor structuredConcurrencyExecutor,
            @Qualifier("consentApiCallExecutor") TaskExecutor apiCallExecutor,
            MeterRegistry meterRegistry,
            ConsentPerformanceMonitor performanceMonitor) {

        this.consentRepository = consentRepository;
        this.queueRepository = queueRepository;
        this.openFinanceClient = openFinanceClient;
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.structuredConcurrencyExecutor = structuredConcurrencyExecutor;
        this.apiCallExecutor = apiCallExecutor;
        this.meterRegistry = meterRegistry;
        this.performanceMonitor = performanceMonitor;
        this.batchSemaphore = new Semaphore(maxConcurrentBatches);

        log.info("VirtualThreadConsentProcessor initialized with batch size: {}, max concurrent: {}", 
                batchSize, maxConcurrentBatches);
    }

    /**
     * Process consent creation with Virtual Thread optimization.
     */
    @Async("consentVirtualThreadExecutor")
    public CompletableFuture<Consent> createConsentAsync(ConsentUseCase.CreateConsentCommand command) {
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Creating consent for organization: {}", command.organizationId());
                
                // Parallel validation if enabled
                if (enableParallelValidation) {
                    return createConsentWithParallelValidation(command, sample);
                } else {
                    return createConsentSequentially(command, sample);
                }
                
            } catch (Exception e) {
                sample.stop(Timer.builder("consent.creation.duration")
                        .tag("status", "error")
                        .register(meterRegistry));
                
                log.error("Error creating consent for organization {}: {}", 
                        command.organizationId(), e.getMessage(), e);
                performanceMonitor.recordError("creation_error", "CREATE_CONSENT", true);
                throw new RuntimeException("Failed to create consent", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Process multiple consent status updates using Structured Concurrency.
     */
    @Async("consentStructuredConcurrencyExecutor")
    public CompletableFuture<BatchConsentProcessingResult> processConsentBatchWithStructuredConcurrency(
            List<UUID> consentIds) {
        
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Processing batch of {} consents with Structured Concurrency", consentIds.size());
                
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    
                    // Submit all consent processing tasks
                    List<StructuredTaskScope.Subtask<ConsentProcessingResult>> subtasks = new ArrayList<>();
                    
                    for (UUID consentId : consentIds) {
                        var subtask = scope.fork(() -> processIndividualConsent(consentId));
                        subtasks.add(subtask);
                    }
                    
                    // Wait for all tasks to complete
                    scope.join();
                    scope.throwIfFailed();
                    
                    // Collect results
                    List<ConsentProcessingResult> results = new ArrayList<>();
                    int successCount = 0;
                    int failureCount = 0;
                    
                    for (var subtask : subtasks) {
                        ConsentProcessingResult result = subtask.get();
                        results.add(result);
                        
                        if (result.success()) {
                            successCount++;
                        } else {
                            failureCount++;
                        }
                    }
                    
                    long duration = sample.stop(Timer.builder("consent.batch.processing.duration")
                            .tag("strategy", "structured-concurrency")
                            .tag("batch_size", String.valueOf(consentIds.size()))
                            .register(meterRegistry));
                    
                    log.debug("Batch processing completed: {} successful, {} failed in {}ms", 
                            successCount, failureCount, duration);
                    
                    // Record metrics
                    performanceMonitor.recordBatchProcessing(successCount + failureCount, duration);
                    
                    return new BatchConsentProcessingResult(
                            consentIds.size(), 
                            successCount, 
                            failureCount, 
                            duration,
                            results
                    );
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Batch processing interrupted", e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("Batch processing failed", e);
                }
                
            } catch (Exception e) {
                sample.stop(Timer.builder("consent.batch.processing.duration")
                        .tag("strategy", "structured-concurrency")
                        .tag("status", "error")
                        .register(meterRegistry));
                
                log.error("Error in batch consent processing", e);
                performanceMonitor.recordError("batch_processing_error", "BATCH_PROCESSING", true);
                throw new RuntimeException("Batch processing failed", e);
            }
        }, structuredConcurrencyExecutor);
    }

    /**
     * Process pending consents with adaptive batch sizing.
     */
    @Async("consentVirtualThreadExecutor")
    public CompletableFuture<Integer> processPendingConsentsWithVirtualThreads() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Starting Virtual Thread consent processing");
                
                int totalProcessed = 0;
                boolean hasMoreConsents = true;
                
                while (hasMoreConsents) {
                    // Get pending consents
                    List<Consent> batch = consentRepository.findByStatusWithLimit(
                            ConsentStatus.AWAITING_AUTHORISATION, batchSize);
                    
                    if (batch.isEmpty()) {
                        hasMoreConsents = false;
                        break;
                    }
                    
                    // Process batch
                    List<UUID> consentIds = batch.stream()
                            .map(Consent::getId)
                            .toList();
                    
                    var batchResult = processConsentBatchWithStructuredConcurrency(consentIds).get();
                    totalProcessed += batchResult.processed();
                    
                    // Rate limiting
                    if (!batchSemaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                        log.warn("Rate limiting consent processing due to high load");
                        break;
                    }
                    batchSemaphore.release();
                }
                
                log.info("Virtual Thread consent processing completed. Total processed: {}", totalProcessed);
                return totalProcessed;
                
            } catch (Exception e) {
                log.error("Error in Virtual Thread consent processing", e);
                performanceMonitor.recordError("batch_processing_error", "VIRTUAL_THREADS", true);
                throw new RuntimeException(e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Revoke multiple consents concurrently using Virtual Threads.
     */
    @Async("consentVirtualThreadExecutor")
    public CompletableFuture<Void> revokeConsentsAsync(List<UUID> consentIds, String revocationReason) {
        return CompletableFuture.runAsync(() -> {
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                List<StructuredTaskScope.Subtask<Void>> revocationTasks = consentIds.stream()
                        .map(consentId -> scope.fork(() -> {
                            revokeIndividualConsent(consentId, revocationReason);
                            return null;
                        }))
                        .toList();
                
                scope.join();
                scope.throwIfFailed();
                
                log.info("Successfully revoked {} consents", consentIds.size());
                
                // Record metrics
                meterRegistry.counter("consent.revocations.batch", "count", String.valueOf(consentIds.size()))
                        .increment();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Consent revocation interrupted", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Consent revocation failed", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Validate consent permissions using parallel processing.
     */
    @Async("consentValidationExecutor")
    public CompletableFuture<ConsentValidationResult> validateConsentPermissionsAsync(
            UUID consentId, Set<String> requestedPermissions) {
        
        return CompletableFuture.supplyAsync(() -> {
            Timer.Sample sample = Timer.Sample.start(meterRegistry);
            
            try {
                Consent consent = consentRepository.findById(consentId)
                        .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + consentId));
                
                // Parallel permission validation if multiple permissions
                if (requestedPermissions.size() > 1 && enableParallelValidation) {
                    return validatePermissionsInParallel(consent, requestedPermissions, sample);
                } else {
                    return validatePermissionsSequentially(consent, requestedPermissions, sample);
                }
                
            } catch (Exception e) {
                sample.stop(Timer.builder("consent.validation.duration")
                        .tag("status", "error")
                        .register(meterRegistry));
                
                log.error("Error validating consent permissions for {}: {}", consentId, e.getMessage(), e);
                performanceMonitor.recordError("validation_error", "VALIDATE_PERMISSIONS", false);
                
                return new ConsentValidationResult(false, "Validation failed: " + e.getMessage(), Set.of());
            }
        }, virtualThreadExecutor);
    }

    /**
     * Reactive consent processing using Project Reactor with Virtual Thread integration.
     */
    public Flux<ConsentProcessingResult> processConsentsReactive(List<UUID> consentIds) {
        return Flux.fromIterable(consentIds)
                .parallel(Math.min(consentIds.size(), maxConcurrentBatches))
                .runOn(reactor.core.scheduler.Schedulers.fromExecutor(virtualThreadExecutor))
                .map(this::processIndividualConsent)
                .doOnNext(result -> {
                    if (result.success()) {
                        log.trace("Consent {} processed successfully", result.consentId());
                    } else {
                        log.warn("Consent {} processing failed: {}", result.consentId(), result.errorMessage());
                    }
                })
                .sequential();
    }

    // Private helper methods

    private Consent createConsentWithParallelValidation(
            ConsentUseCase.CreateConsentCommand command, Timer.Sample sample) {
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            // Parallel tasks for consent creation
            var organizationValidationTask = scope.fork(() -> validateOrganization(command.organizationId()));
            var customerValidationTask = scope.fork(() -> validateCustomer(command.customerId()));
            var permissionValidationTask = scope.fork(() -> validatePermissions(command.permissions()));
            
            scope.join();
            scope.throwIfFailed();
            
            // All validations passed, create consent
            Consent consent = buildConsent(command);
            consent = consentRepository.save(consent);
            
            // Create processing job asynchronously
            createProcessingJobAsync(consent);
            
            sample.stop(Timer.builder("consent.creation.duration")
                    .tag("strategy", "parallel-validation")
                    .tag("status", "success")
                    .register(meterRegistry));
            
            performanceMonitor.recordConsentOperation("CREATE", true, sample.stop());
            return consent;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Consent creation interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Consent creation validation failed", e);
        }
    }

    private Consent createConsentSequentially(ConsentUseCase.CreateConsentCommand command, Timer.Sample sample) {
        // Sequential validation for simpler cases
        validateOrganization(command.organizationId());
        validateCustomer(command.customerId());
        validatePermissions(command.permissions());
        
        Consent consent = buildConsent(command);
        consent = consentRepository.save(consent);
        
        createProcessingJobAsync(consent);
        
        long duration = sample.stop(Timer.builder("consent.creation.duration")
                .tag("strategy", "sequential")
                .tag("status", "success")
                .register(meterRegistry));
        
        performanceMonitor.recordConsentOperation("CREATE", true, duration);
        return consent;
    }

    private ConsentProcessingResult processIndividualConsent(UUID consentId) {
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try {
            Consent consent = consentRepository.findById(consentId).orElse(null);
            if (consent == null) {
                return new ConsentProcessingResult(consentId, false, "Consent not found", null);
            }
            
            // Call external API with timeout
            CompletableFuture<ConsentResponse> apiFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return openFinanceClient.getConsent(consent.getOrganizationId(), consent.getConsentId());
                } catch (Exception e) {
                    throw new RuntimeException("API call failed", e);
                }
            }, apiCallExecutor);
            
            ConsentResponse response = apiFuture.get(apiCallTimeout.toMillis(), TimeUnit.MILLISECONDS);
            
            // Update consent status
            ConsentStatus newStatus = mapStatus(response.getStatus());
            consentRepository.updateStatus(consentId, newStatus);
            
            long duration = sample.stop(Timer.builder("consent.processing.duration")
                    .tag("status", "success")
                    .register(meterRegistry));
            
            performanceMonitor.recordConsentOperation("PROCESS", true, duration);
            
            return new ConsentProcessingResult(consentId, true, null, newStatus);
            
        } catch (TimeoutException e) {
            sample.stop(Timer.builder("consent.processing.duration")
                    .tag("status", "timeout")
                    .register(meterRegistry));
            
            performanceMonitor.recordError("timeout", "PROCESS_CONSENT", true);
            return new ConsentProcessingResult(consentId, false, "API call timeout", null);
            
        } catch (Exception e) {
            sample.stop(Timer.builder("consent.processing.duration")
                    .tag("status", "error")
                    .register(meterRegistry));
            
            log.error("Error processing consent {}: {}", consentId, e.getMessage(), e);
            performanceMonitor.recordError("processing_error", "PROCESS_CONSENT", true);
            
            return new ConsentProcessingResult(consentId, false, e.getMessage(), null);
        }
    }

    private ConsentValidationResult validatePermissionsInParallel(
            Consent consent, Set<String> requestedPermissions, Timer.Sample sample) {
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            
            List<StructuredTaskScope.Subtask<Boolean>> validationTasks = requestedPermissions.stream()
                    .map(permission -> scope.fork(() -> validateSinglePermission(consent, permission)))
                    .toList();
            
            scope.join();
            scope.throwIfFailed();
            
            // Check all validation results
            Set<String> validPermissions = requestedPermissions.stream()
                    .filter(permission -> validateSinglePermission(consent, permission))
                    .collect(java.util.stream.Collectors.toSet());
            
            boolean allValid = validPermissions.size() == requestedPermissions.size();
            
            long duration = sample.stop(Timer.builder("consent.validation.duration")
                    .tag("strategy", "parallel")
                    .tag("status", allValid ? "success" : "partial")
                    .register(meterRegistry));
            
            performanceMonitor.recordConsentOperation("VALIDATE", allValid, duration);
            
            return new ConsentValidationResult(
                    allValid,
                    allValid ? "All permissions valid" : "Some permissions invalid",
                    validPermissions
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Permission validation interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Permission validation failed", e);
        }
    }

    private ConsentValidationResult validatePermissionsSequentially(
            Consent consent, Set<String> requestedPermissions, Timer.Sample sample) {
        
        Set<String> validPermissions = requestedPermissions.stream()
                .filter(permission -> validateSinglePermission(consent, permission))
                .collect(java.util.stream.Collectors.toSet());
        
        boolean allValid = validPermissions.size() == requestedPermissions.size();
        
        long duration = sample.stop(Timer.builder("consent.validation.duration")
                .tag("strategy", "sequential")
                .tag("status", allValid ? "success" : "partial")
                .register(meterRegistry));
        
        performanceMonitor.recordConsentOperation("VALIDATE", allValid, duration);
        
        return new ConsentValidationResult(
                allValid,
                allValid ? "All permissions valid" : "Some permissions invalid",
                validPermissions
        );
    }

    // Helper methods for validation and processing
    private boolean validateOrganization(String organizationId) {
        // Implementation for organization validation
        return organizationId != null && !organizationId.trim().isEmpty();
    }

    private boolean validateCustomer(String customerId) {
        // Implementation for customer validation
        return customerId != null && !customerId.trim().isEmpty();
    }

    private boolean validatePermissions(Set<String> permissions) {
        // Implementation for permissions validation
        return permissions != null && !permissions.isEmpty();
    }

    private boolean validateSinglePermission(Consent consent, String permission) {
        // Check if consent has the requested permission
        return consent.getPermissions().stream()
                .anyMatch(p -> p.name().equals(permission));
    }

    private Consent buildConsent(ConsentUseCase.CreateConsentCommand command) {
        return Consent.builder()
                .id(UUID.randomUUID())
                .consentId(UUID.randomUUID().toString())
                .organizationId(command.organizationId())
                .customerId(command.customerId())
                .permissions(mapPermissions(command.permissions()))
                .status(ConsentStatus.AWAITING_AUTHORISATION)
                .createdAt(LocalDateTime.now())
                .expirationDateTime(command.expirationDateTime())
                .build();
    }

    private Set<br.com.openfinance.domain.consent.Permission> mapPermissions(Set<String> permissions) {
        return permissions.stream()
                .map(this::mapPermission)
                .collect(java.util.stream.Collectors.toSet());
    }

    private br.com.openfinance.domain.consent.Permission mapPermission(String permission) {
        return switch (permission) {
            case "ACCOUNTS_READ" -> br.com.openfinance.domain.consent.Permission.ACCOUNTS_READ;
            case "ACCOUNTS_BALANCES_READ" -> br.com.openfinance.domain.consent.Permission.ACCOUNTS_BALANCES_READ;
            case "ACCOUNTS_TRANSACTIONS_READ" -> br.com.openfinance.domain.consent.Permission.ACCOUNTS_TRANSACTIONS_READ;
            case "ACCOUNTS_OVERDRAFT_LIMITS_READ" -> br.com.openfinance.domain.consent.Permission.ACCOUNTS_OVERDRAFT_LIMITS_READ;
            case "CREDIT_CARDS_ACCOUNTS_READ" -> br.com.openfinance.domain.consent.Permission.CREDIT_CARDS_ACCOUNTS_READ;
            case "CREDIT_CARDS_ACCOUNTS_LIMITS_READ" -> br.com.openfinance.domain.consent.Permission.CREDIT_CARDS_ACCOUNTS_LIMITS_READ;
            case "CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ" -> br.com.openfinance.domain.consent.Permission.CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ;
            case "CREDIT_CARDS_ACCOUNTS_BILLS_READ" -> br.com.openfinance.domain.consent.Permission.CREDIT_CARDS_ACCOUNTS_BILLS_READ;
            case "RESOURCES_READ" -> br.com.openfinance.domain.consent.Permission.RESOURCES_READ;
            default -> throw new IllegalArgumentException("Unknown permission: " + permission);
        };
    }

    private ConsentStatus mapStatus(String status) {
        return switch (status) {
            case "AWAITING_AUTHORISATION" -> ConsentStatus.AWAITING_AUTHORISATION;
            case "AUTHORISED" -> ConsentStatus.AUTHORISED;
            case "REJECTED" -> ConsentStatus.REJECTED;
            case "CONSUMED" -> ConsentStatus.CONSUMED;
            case "EXPIRED" -> ConsentStatus.EXPIRED;
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }

    private void createProcessingJobAsync(Consent consent) {
        CompletableFuture.runAsync(() -> {
            try {
                ProcessingJob job = ProcessingJob.builder()
                        .consentId(consent.getId())
                        .organizationId(consent.getOrganizationId())
                        .status(JobStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();
                
                queueRepository.save(job);
                
            } catch (Exception e) {
                log.error("Failed to create processing job for consent {}: {}", 
                        consent.getId(), e.getMessage(), e);
            }
        }, virtualThreadExecutor);
    }

    private void revokeIndividualConsent(UUID consentId, String revocationReason) {
        try {
            consentRepository.updateStatus(consentId, ConsentStatus.REJECTED);
            
            // Log revocation
            log.info("Consent {} revoked. Reason: {}", consentId, revocationReason);
            
            // Create revocation job if needed
            // Implementation specific logic here
            
        } catch (Exception e) {
            log.error("Failed to revoke consent {}: {}", consentId, e.getMessage(), e);
            throw new RuntimeException("Consent revocation failed", e);
        }
    }

    // Result records
    public record ConsentProcessingResult(
            UUID consentId,
            boolean success,
            String errorMessage,
            ConsentStatus newStatus
    ) {}

    public record BatchConsentProcessingResult(
            int processed,
            int successful,
            int failed,
            long durationMs,
            List<ConsentProcessingResult> results
    ) {}

    public record ConsentValidationResult(
            boolean valid,
            String message,
            Set<String> validPermissions
    ) {}
}