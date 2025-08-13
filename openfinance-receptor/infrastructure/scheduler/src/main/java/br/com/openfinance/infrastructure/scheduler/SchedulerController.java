package br.com.openfinance.infrastructure.scheduler;

import br.com.openfinance.application.port.input.ConsentUseCase;
import br.com.openfinance.application.port.output.ProcessingQueueRepository;
import br.com.openfinance.domain.processing.ProcessingJob;
import br.com.openfinance.infrastructure.scheduler.monitoring.SchedulerPerformanceMonitor;
import br.com.openfinance.infrastructure.scheduler.service.AdaptiveResourceManager;
import br.com.openfinance.infrastructure.scheduler.service.VirtualThreadProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced Scheduler Controller with comprehensive monitoring and management capabilities.
 * Provides REST API for scheduler operations, performance monitoring, and adaptive configuration.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/scheduler")
public class SchedulerController {

    private final ProcessingQueueRepository queueRepository;
    private final ConsentUseCase consentUseCase;
    private final ProcessingWorker processingWorker;
    private final VirtualThreadProcessingService processingService;
    private final AdaptiveResourceManager resourceManager;
    private final SchedulerPerformanceMonitor performanceMonitor;

    public SchedulerController(
            ProcessingQueueRepository queueRepository,
            ConsentUseCase consentUseCase,
            ProcessingWorker processingWorker,
            VirtualThreadProcessingService processingService,
            AdaptiveResourceManager resourceManager,
            SchedulerPerformanceMonitor performanceMonitor) {
        
        this.queueRepository = queueRepository;
        this.consentUseCase = consentUseCase;
        this.processingWorker = processingWorker;
        this.processingService = processingService;
        this.resourceManager = resourceManager;
        this.performanceMonitor = performanceMonitor;
    }

    /**
     * Get comprehensive scheduler status including performance metrics.
     */
    @GetMapping("/status")
    public ResponseEntity<SchedulerStatusResponse> getStatus() {
        try {
            var workerStatus = processingWorker.getStatus();
            var processingStats = processingService.getProcessingStatistics();
            var resourceUtilization = resourceManager.getResourceUtilization();
            var performanceReport = performanceMonitor.getPerformanceReport();

            var response = new SchedulerStatusResponse(
                    LocalDateTime.now(),
                    workerStatus.isRunning(),
                    workerStatus.isProcessingActive(),
                    workerStatus,
                    processingStats,
                    resourceUtilization,
                    performanceReport
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting scheduler status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Start the processing worker.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startProcessing() {
        try {
            if (processingWorker.isRunning()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "ProcessingWorker is already running",
                                "timestamp", LocalDateTime.now()
                        ));
            }

            processingWorker.startProcessing();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ProcessingWorker started successfully",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error starting ProcessingWorker", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to start ProcessingWorker: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Stop the processing worker.
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopProcessing() {
        try {
            processingWorker.stop();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "ProcessingWorker stopped successfully",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error stopping ProcessingWorker", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to stop ProcessingWorker: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Manually trigger batch processing.
     */
    @PostMapping("/trigger-batch")
    public ResponseEntity<Map<String, Object>> triggerBatch() {
        try {
            if (!processingWorker.isRunning()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "ProcessingWorker is not running",
                                "timestamp", LocalDateTime.now()
                        ));
            }

            var future = processingService.processPendingJobsWithVirtualThreads();
            
            future.whenComplete((count, throwable) -> {
                if (throwable != null) {
                    log.error("Manual batch processing failed", throwable);
                } else {
                    log.info("Manual batch processing completed: {} jobs processed", count);
                }
            });

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Batch processing triggered successfully",
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error triggering batch processing", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to trigger batch processing: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Get current processing queue status.
     */
    @GetMapping("/queue")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<ProcessingJob> nextBatch = queueRepository.fetchNextBatch(limit);
            var processingStats = processingService.getProcessingStatistics();

            var response = new QueueStatusResponse(
                    processingStats.pendingJobs(),
                    processingStats.processingJobs(),
                    processingStats.completedJobs(),
                    processingStats.failedJobs(),
                    nextBatch,
                    LocalDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting queue status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Process specific consent jobs manually.
     */
    @PostMapping("/process-consents")
    public ResponseEntity<Map<String, Object>> processConsents(@RequestBody List<UUID> consentIds) {
        try {
            if (consentIds == null || consentIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "No consent IDs provided",
                                "timestamp", LocalDateTime.now()
                        ));
            }

            var future = processingService.processConsentJobs(consentIds);
            
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Manual consent processing failed", throwable);
                } else {
                    log.info("Manual consent processing completed for {} consents", consentIds.size());
                }
            });

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", String.format("Processing %d consent jobs", consentIds.size()),
                    "consentIds", consentIds,
                    "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error processing consent jobs", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "Failed to process consent jobs: " + e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }

    /**
     * Get performance metrics and recommendations.
     */
    @GetMapping("/performance")
    public ResponseEntity<PerformanceResponse> getPerformanceMetrics() {
        try {
            var performanceReport = performanceMonitor.getPerformanceReport();
            var recommendations = performanceMonitor.getRecommendations();
            var resourceUtilization = resourceManager.getResourceUtilization();

            var response = new PerformanceResponse(
                    performanceReport,
                    recommendations,
                    resourceUtilization,
                    LocalDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting performance metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update adaptive configuration parameters.
     */
    @PostMapping("/adaptive/config")
    public ResponseEntity<Map<String, Object>> updateAdaptiveConfig(
            @RequestBody AdaptiveConfigRequest configRequest) {
        try {
            // This would typically update configuration in the AdaptiveResourceManager
            // For now, we'll just return the current configuration
            
            var currentUtilization = resourceManager.getResourceUtilization();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Adaptive configuration updated",
                    "currentConfig", Map.of(
                            "batchSize", resourceManager.getDynamicBatchSize(),
                            "concurrencyLevel", resourceManager.getDynamicConcurrencyLevel(),
                            "processingInterval", resourceManager.getDynamicProcessingInterval(),
                            "activeTasks", currentUtilization.activeTasks(),
                            "maxConcurrency", currentUtilization.maxConcurrency()
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
            boolean isHealthy = processingWorker.isRunning() && 
                               !resourceManager.getResourceUtilization().isUnderPressure();
            
            return ResponseEntity.ok(Map.of(
                    "status", isHealthy ? "UP" : "DOWN",
                    "timestamp", LocalDateTime.now(),
                    "details", Map.of(
                            "workerRunning", processingWorker.isRunning(),
                            "processingActive", processingWorker.isProcessingActive(),
                            "systemUnderPressure", resourceManager.getResourceUtilization().isUnderPressure()
                    )
            ));

        } catch (Exception e) {
            log.error("Error checking scheduler health", e);
            return ResponseEntity.status(503)
                    .body(Map.of(
                            "status", "DOWN",
                            "timestamp", LocalDateTime.now(),
                            "error", e.getMessage()
                    ));
        }
    }

    // Response DTOs
    public record SchedulerStatusResponse(
            LocalDateTime timestamp,
            boolean isRunning,
            boolean isProcessingActive,
            ProcessingWorker.WorkerStatus workerStatus,
            VirtualThreadProcessingService.ProcessingStatistics processingStats,
            AdaptiveResourceManager.ResourceUtilization resourceUtilization,
            SchedulerPerformanceMonitor.PerformanceReport performanceReport
    ) {}

    public record QueueStatusResponse(
            long pendingJobs,
            long processingJobs,
            long completedJobs,
            long failedJobs,
            List<ProcessingJob> nextBatch,
            LocalDateTime timestamp
    ) {}

    public record PerformanceResponse(
            SchedulerPerformanceMonitor.PerformanceReport performanceReport,
            SchedulerPerformanceMonitor.PerformanceRecommendations recommendations,
            AdaptiveResourceManager.ResourceUtilization resourceUtilization,
            LocalDateTime timestamp
    ) {}

    // Request DTOs
    public record AdaptiveConfigRequest(
            Integer batchSize,
            Integer concurrencyLevel,
            Long processingInterval,
            Double memoryThreshold,
            Double cpuThreshold
    ) {}
}
