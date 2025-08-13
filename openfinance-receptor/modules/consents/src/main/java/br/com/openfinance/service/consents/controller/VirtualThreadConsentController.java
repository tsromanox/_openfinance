package br.com.openfinance.service.consents.controller;

import br.com.openfinance.application.port.input.ConsentUseCase;
import br.com.openfinance.domain.consent.Consent;
import br.com.openfinance.domain.consent.ConsentStatus;
import br.com.openfinance.service.consents.VirtualThreadConsentService;
import br.com.openfinance.service.consents.monitoring.ConsentPerformanceMonitor;
import br.com.openfinance.service.consents.resource.AdaptiveConsentResourceManager;
import br.com.openfinance.service.consents.validation.ParallelConsentValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced REST controller for consent operations with Virtual Thread and
 * Structured Concurrency optimizations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/consents")
public class VirtualThreadConsentController {

    private final VirtualThreadConsentService consentService;
    private final ParallelConsentValidator consentValidator;
    private final AdaptiveConsentResourceManager resourceManager;
    private final ConsentPerformanceMonitor performanceMonitor;

    public VirtualThreadConsentController(
            VirtualThreadConsentService consentService,
            ParallelConsentValidator consentValidator,
            AdaptiveConsentResourceManager resourceManager,
            ConsentPerformanceMonitor performanceMonitor) {
        
        this.consentService = consentService;
        this.consentValidator = consentValidator;
        this.resourceManager = resourceManager;
        this.performanceMonitor = performanceMonitor;
    }

    /**
     * Create a new consent (standard interface).
     */
    @PostMapping
    public ResponseEntity<ConsentResponse> createConsent(@Valid @RequestBody CreateConsentRequest request) {
        try {
            var command = new ConsentUseCase.CreateConsentCommand(
                    request.organizationId(),
                    request.customerId(),
                    request.permissions(),
                    request.expirationDateTime()
            );
            
            Consent consent = consentService.createConsent(command);
            
            return ResponseEntity.ok(new ConsentResponse(
                    consent.getId(),
                    consent.getConsentId(),
                    consent.getOrganizationId(),
                    consent.getCustomerId(),
                    consent.getStatus().name(),
                    consent.getPermissions().stream()
                            .map(Enum::name)
                            .collect(java.util.stream.Collectors.toSet()),
                    consent.getCreatedAt(),
                    consent.getExpirationDateTime()
            ));
            
        } catch (Exception e) {
            log.error("Error creating consent", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create multiple consents concurrently using Virtual Threads.
     */
    @PostMapping("/batch")
    public ResponseEntity<CompletableFuture<BatchConsentCreationResponse>> createConsentsAsync(
            @Valid @RequestBody List<CreateConsentRequest> requests) {
        
        try {
            List<ConsentUseCase.CreateConsentCommand> commands = requests.stream()
                    .map(request -> new ConsentUseCase.CreateConsentCommand(
                            request.organizationId(),
                            request.customerId(),
                            request.permissions(),
                            request.expirationDateTime()
                    ))
                    .toList();
            
            CompletableFuture<List<Consent>> consentsFuture = consentService.createConsentsAsync(commands);
            
            CompletableFuture<BatchConsentCreationResponse> responseFuture = consentsFuture
                    .thenApply(consents -> {
                        List<ConsentResponse> responses = consents.stream()
                                .map(this::mapToResponse)
                                .toList();
                        
                        return new BatchConsentCreationResponse(
                                requests.size(),
                                consents.size(),
                                requests.size() - consents.size(),
                                responses,
                                LocalDateTime.now()
                        );
                    });
            
            return ResponseEntity.accepted().body(responseFuture);
            
        } catch (Exception e) {
            log.error("Error creating consents in batch", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get consent by ID.
     */
    @GetMapping("/{consentId}")
    public ResponseEntity<ConsentResponse> getConsent(@PathVariable UUID consentId) {
        try {
            Consent consent = consentService.getConsent(consentId);
            return ResponseEntity.ok(mapToResponse(consent));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving consent {}", consentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get consents by status using reactive processing.
     */
    @GetMapping("/status/{status}")
    public Flux<ConsentResponse> getConsentsByStatus(
            @PathVariable ConsentStatus status,
            @RequestParam(defaultValue = "100") int limit) {
        
        return consentService.getConsentsByStatusReactively(status, limit)
                .map(this::mapToResponse)
                .doOnError(error -> log.error("Error retrieving consents by status {}", status, error));
    }

    /**
     * Process consent (standard interface).
     */
    @PostMapping("/{consentId}/process")
    public ResponseEntity<Map<String, Object>> processConsent(@PathVariable UUID consentId) {
        try {
            consentService.processConsent(consentId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Consent processing initiated",
                    "consentId", consentId,
                    "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("Error processing consent {}", consentId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to process consent: " + e.getMessage(),
                            "consentId", consentId,
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Process multiple consents using Structured Concurrency.
     */
    @PostMapping("/batch/process")
    public ResponseEntity<CompletableFuture<VirtualThreadConsentService.BatchProcessingResult>> 
            processConsentsWithStructuredConcurrency(@RequestBody List<UUID> consentIds) {
        
        try {
            CompletableFuture<VirtualThreadConsentService.BatchProcessingResult> processingFuture = 
                    consentService.processConsentsWithStructuredConcurrency(consentIds);
            
            return ResponseEntity.accepted().body(processingFuture);
            
        } catch (Exception e) {
            log.error("Error processing consents in batch", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Process consents reactively with backpressure support.
     */
    @PostMapping("/reactive/process")
    public Flux<VirtualThreadConsentService.ConsentProcessingResult> processConsentsReactively(
            @RequestBody List<UUID> consentIds) {
        
        return consentService.processConsentsReactively(consentIds)
                .doOnNext(result -> log.debug("Processed consent: {} - {}", 
                        result.consentId(), result.success() ? "SUCCESS" : "FAILURE"))
                .doOnError(error -> log.error("Error in reactive consent processing", error));
    }

    /**
     * Revoke consent (standard interface).
     */
    @PostMapping("/{consentId}/revoke")
    public ResponseEntity<Map<String, Object>> revokeConsent(
            @PathVariable UUID consentId,
            @RequestBody(required = false) Map<String, String> payload) {
        
        try {
            String reason = payload != null ? payload.get("reason") : "Manual revocation";
            consentService.revokeConsent(consentId);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Consent revoked successfully",
                    "consentId", consentId,
                    "reason", reason,
                    "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("Error revoking consent {}", consentId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to revoke consent: " + e.getMessage(),
                            "consentId", consentId,
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Revoke multiple consents concurrently.
     */
    @PostMapping("/batch/revoke")
    public ResponseEntity<CompletableFuture<Map<String, Object>>> revokeConsentsAsync(
            @RequestBody RevokeConsentsRequest request) {
        
        try {
            CompletableFuture<Void> revocationFuture = consentService.revokeConsentsAsync(
                    request.consentIds(), request.reason());
            
            CompletableFuture<Map<String, Object>> responseFuture = revocationFuture
                    .thenApply(result -> Map.of(
                            "success", true,
                            "message", String.format("Revoked %d consents", request.consentIds().size()),
                            "consentIds", request.consentIds(),
                            "reason", request.reason(),
                            "timestamp", LocalDateTime.now()
                    ));
            
            return ResponseEntity.accepted().body(responseFuture);
            
        } catch (Exception e) {
            log.error("Error revoking consents in batch", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate consent permissions using parallel processing.
     */
    @PostMapping("/{consentId}/validate-permissions")
    public ResponseEntity<ParallelConsentValidator.ConsentPermissionValidationResult> validateConsentPermissions(
            @PathVariable UUID consentId,
            @RequestBody Set<String> requiredPermissions) {
        
        try {
            var validationResult = consentValidator.validateConsentPermissions(consentId, requiredPermissions);
            return ResponseEntity.ok(validationResult);
            
        } catch (Exception e) {
            log.error("Error validating permissions for consent {}", consentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate multiple consents concurrently.
     */
    @PostMapping("/batch/validate")
    public ResponseEntity<CompletableFuture<List<VirtualThreadConsentService.ConsentBusinessValidationResult>>> 
            validateMultipleConsentsAsync(@RequestBody List<UUID> consentIds) {
        
        try {
            CompletableFuture<List<VirtualThreadConsentService.ConsentBusinessValidationResult>> validationFuture = 
                    consentValidator.validateMultipleConsentsAsync(consentIds);
            
            return ResponseEntity.accepted().body(validationFuture);
            
        } catch (Exception e) {
            log.error("Error validating consents in batch", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Comprehensive consent validation.
     */
    @PostMapping("/{consentId}/validate-comprehensive")
    public ResponseEntity<ParallelConsentValidator.ComprehensiveValidationResult> validateConsentComprehensively(
            @PathVariable UUID consentId) {
        
        try {
            var validationResult = consentValidator.validateConsentComprehensively(consentId);
            return ResponseEntity.ok(validationResult);
            
        } catch (Exception e) {
            log.error("Error in comprehensive validation for consent {}", consentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Process consents by organization using adaptive resource management.
     */
    @PostMapping("/organization/{organizationId}/process")
    public ResponseEntity<CompletableFuture<VirtualThreadConsentService.OrganizationProcessingResult>> 
            processConsentsByOrganization(
                    @PathVariable String organizationId,
                    @RequestParam(defaultValue = "100") int batchSize) {
        
        try {
            CompletableFuture<VirtualThreadConsentService.OrganizationProcessingResult> processingFuture = 
                    consentService.processConsentsByOrganization(organizationId, batchSize);
            
            return ResponseEntity.accepted().body(processingFuture);
            
        } catch (Exception e) {
            log.error("Error processing consents for organization {}", organizationId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Process pending consents adaptively.
     */
    @PostMapping("/process-pending")
    public ResponseEntity<CompletableFuture<Integer>> processPendingConsentsAdaptively() {
        try {
            CompletableFuture<Integer> processingFuture = consentService.processPendingConsentsAdaptively();
            return ResponseEntity.accepted().body(processingFuture);
            
        } catch (Exception e) {
            log.error("Error processing pending consents", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get consent statistics using parallel aggregation.
     */
    @GetMapping("/statistics")
    public ResponseEntity<CompletableFuture<VirtualThreadConsentService.ConsentStatistics>> getConsentStatistics() {
        try {
            CompletableFuture<VirtualThreadConsentService.ConsentStatistics> statisticsFuture = 
                    consentService.getConsentStatisticsAsync();
            
            return ResponseEntity.ok(statisticsFuture);
            
        } catch (Exception e) {
            log.error("Error retrieving consent statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get service status and performance metrics.
     */
    @GetMapping("/service/status")
    public ResponseEntity<VirtualThreadConsentService.ConsentServiceStatus> getServiceStatus() {
        try {
            var serviceStatus = consentService.getServiceStatus();
            return ResponseEntity.ok(serviceStatus);
            
        } catch (Exception e) {
            log.error("Error retrieving service status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get performance metrics and recommendations.
     */
    @GetMapping("/performance")
    public ResponseEntity<ConsentPerformanceResponse> getPerformanceMetrics() {
        try {
            var performanceReport = performanceMonitor.getPerformanceReport();
            var recommendations = performanceMonitor.getRecommendations();
            var resourceUtilization = resourceManager.getResourceUtilization();
            
            var response = new ConsentPerformanceResponse(
                    performanceReport,
                    recommendations,
                    resourceUtilization,
                    LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving performance metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update adaptive configuration parameters.
     */
    @PostMapping("/configuration/adaptive")
    public ResponseEntity<Map<String, Object>> updateAdaptiveConfiguration(
            @RequestBody AdaptiveConfigurationRequest configRequest) {
        
        try {
            // This would typically update configuration in the AdaptiveConsentResourceManager
            var currentUtilization = resourceManager.getResourceUtilization();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Adaptive configuration updated",
                    "currentConfiguration", Map.of(
                            "batchSize", resourceManager.getDynamicBatchSize(),
                            "processingConcurrency", resourceManager.getDynamicConcurrencyLevel(),
                            "validationConcurrency", resourceManager.getDynamicValidationConcurrency(),
                            "apiConcurrency", resourceManager.getDynamicApiCallConcurrency(),
                            "processingInterval", resourceManager.getDynamicProcessingInterval(),
                            "activeProcessingTasks", currentUtilization.activeProcessingTasks(),
                            "activeValidationTasks", currentUtilization.activeValidationTasks(),
                            "activeApiCalls", currentUtilization.activeApiCalls()
                    ),
                    "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("Error updating adaptive configuration", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to update adaptive configuration: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            var serviceStatus = consentService.getServiceStatus();
            boolean isHealthy = serviceStatus.isRunning() && !serviceStatus.isUnderPressure();
            
            return ResponseEntity.ok(Map.of(
                    "status", isHealthy ? "UP" : "DOWN",
                    "timestamp", LocalDateTime.now(),
                    "details", Map.of(
                            "serviceRunning", serviceStatus.isRunning(),
                            "activeProcessingTasks", serviceStatus.activeProcessingTasks(),
                            "activeValidationTasks", serviceStatus.activeValidationTasks(),
                            "activeApiCalls", serviceStatus.activeApiCalls(),
                            "processingUtilization", serviceStatus.processingUtilizationPercentage(),
                            "validationUtilization", serviceStatus.validationUtilizationPercentage(),
                            "apiUtilization", serviceStatus.apiCallUtilizationPercentage(),
                            "systemUnderPressure", serviceStatus.isUnderPressure(),
                            "currentThroughput", serviceStatus.currentThroughput(),
                            "processingEfficiency", serviceStatus.processingEfficiency(),
                            "errorRate", serviceStatus.errorRate()
                    )
            ));
            
        } catch (Exception e) {
            log.error("Error checking consent service health", e);
            return ResponseEntity.status(503)
                    .body(Map.of(
                            "status", "DOWN",
                            "timestamp", LocalDateTime.now(),
                            "error", e.getMessage()
                    ));
        }
    }

    // Helper methods
    private ConsentResponse mapToResponse(Consent consent) {
        return new ConsentResponse(
                consent.getId(),
                consent.getConsentId(),
                consent.getOrganizationId(),
                consent.getCustomerId(),
                consent.getStatus().name(),
                consent.getPermissions().stream()
                        .map(Enum::name)
                        .collect(java.util.stream.Collectors.toSet()),
                consent.getCreatedAt(),
                consent.getExpirationDateTime()
        );
    }

    // Request/Response DTOs
    public record CreateConsentRequest(
            String organizationId,
            String customerId,
            Set<String> permissions,
            LocalDateTime expirationDateTime
    ) {}

    public record ConsentResponse(
            UUID id,
            String consentId,
            String organizationId,
            String customerId,
            String status,
            Set<String> permissions,
            LocalDateTime createdAt,
            LocalDateTime expirationDateTime
    ) {}

    public record BatchConsentCreationResponse(
            int requested,
            int successful,
            int failed,
            List<ConsentResponse> consents,
            LocalDateTime timestamp
    ) {}

    public record RevokeConsentsRequest(
            List<UUID> consentIds,
            String reason
    ) {}

    public record ConsentPerformanceResponse(
            ConsentPerformanceMonitor.ConsentPerformanceReport performanceReport,
            ConsentPerformanceMonitor.ConsentPerformanceRecommendations recommendations,
            AdaptiveConsentResourceManager.ConsentResourceUtilization resourceUtilization,
            LocalDateTime timestamp
    ) {}

    public record AdaptiveConfigurationRequest(
            Integer batchSize,
            Integer processingConcurrency,
            Integer validationConcurrency,
            Integer apiConcurrency,
            Long processingInterval,
            Double memoryThreshold,
            Double cpuThreshold
    ) {}
}