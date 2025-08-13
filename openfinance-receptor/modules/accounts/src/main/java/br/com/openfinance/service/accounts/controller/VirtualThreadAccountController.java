package br.com.openfinance.service.accounts.controller;

import br.com.openfinance.service.accounts.VirtualThreadAccountService;
import br.com.openfinance.service.accounts.monitoring.AccountPerformanceMonitor;
import br.com.openfinance.service.accounts.resource.AdaptiveAccountResourceManager;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for Virtual Thread optimized account processing operations.
 * Provides comprehensive account management APIs with high-performance processing.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
public class VirtualThreadAccountController {
    
    private final VirtualThreadAccountService accountService;
    private final AccountPerformanceMonitor performanceMonitor;
    private final AdaptiveAccountResourceManager resourceManager;
    
    public VirtualThreadAccountController(
            VirtualThreadAccountService accountService,
            AccountPerformanceMonitor performanceMonitor,
            AdaptiveAccountResourceManager resourceManager) {
        this.accountService = accountService;
        this.performanceMonitor = performanceMonitor;
        this.resourceManager = resourceManager;
    }
    
    /**
     * Sync accounts for a single consent.
     */
    @PostMapping("/sync/{consentId}")
    @Timed(name = "accounts.sync.single", description = "Time taken to sync accounts for a single consent")
    public ResponseEntity<AccountSyncResponse> syncAccountsForConsent(@PathVariable UUID consentId) {
        try {
            log.info("Starting account sync for consent: {}", consentId);
            
            var accounts = accountService.syncAccountsForConsent(consentId);
            
            var response = new AccountSyncResponse(
                    consentId,
                    accounts.size(),
                    true,
                    "Accounts synchronized successfully",
                    accounts.stream().map(account -> new AccountSummary(
                            account.getAccountId(),
                            account.getType(),
                            account.getSubtype(),
                            account.getBrandName()
                    )).toList()
            );
            
            log.info("Successfully synced {} accounts for consent {}", accounts.size(), consentId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error syncing accounts for consent {}: {}", consentId, e.getMessage());
            
            var response = new AccountSyncResponse(
                    consentId,
                    0,
                    false,
                    e.getMessage(),
                    List.of()
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Sync accounts for multiple consents asynchronously.
     */
    @PostMapping("/sync/batch")
    @Timed(name = "accounts.sync.batch", description = "Time taken to sync accounts for multiple consents")
    public CompletableFuture<ResponseEntity<BatchSyncResponse>> syncAccountsForConsentsAsync(
            @RequestBody BatchSyncRequest request) {
        
        log.info("Starting batch account sync for {} consents", request.consentIds().size());
        
        return accountService.syncAccountsForConsentsAsync(request.consentIds())
                .thenApply(accounts -> {
                    var response = new BatchSyncResponse(
                            request.consentIds().size(),
                            accounts.size(),
                            true,
                            "Batch sync completed successfully",
                            accounts.stream().map(account -> new AccountSummary(
                                    account.getAccountId(),
                                    account.getType(),
                                    account.getSubtype(),
                                    account.getBrandName()
                            )).toList()
                    );
                    
                    log.info("Batch sync completed: {} accounts from {} consents", 
                            accounts.size(), request.consentIds().size());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Batch sync failed: {}", e.getMessage());
                    
                    var response = new BatchSyncResponse(
                            request.consentIds().size(),
                            0,
                            false,
                            e.getMessage(),
                            List.of()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Process accounts with adaptive batch sizing.
     */
    @PostMapping("/process/adaptive")
    @Timed(name = "accounts.process.adaptive", description = "Time taken for adaptive account processing")
    public CompletableFuture<ResponseEntity<AdaptiveProcessingResponse>> processAccountsAdaptively(
            @RequestBody BatchSyncRequest request) {
        
        log.info("Starting adaptive account processing for {} consents", request.consentIds().size());
        
        return accountService.processAccountsInAdaptiveBatches(request.consentIds())
                .thenApply(result -> {
                    var response = new AdaptiveProcessingResponse(
                            result.processedCount(),
                            result.errorCount(),
                            result.durationMs(),
                            result.strategy(),
                            result.batchSize(),
                            result.concurrencyLevel(),
                            true,
                            "Adaptive processing completed successfully"
                    );
                    
                    log.info("Adaptive processing completed: {} processed, {} errors in {}ms", 
                            result.processedCount(), result.errorCount(), result.durationMs());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Adaptive processing failed: {}", e.getMessage());
                    
                    var response = new AdaptiveProcessingResponse(
                            0, 0, 0, "FAILED", 0, 0, false, e.getMessage()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Process massive account workloads with Virtual Thread scalability.
     */
    @PostMapping("/process/massive")
    @Timed(name = "accounts.process.massive", description = "Time taken for massive account processing")
    public CompletableFuture<ResponseEntity<MassiveProcessingResponse>> processMassiveWorkload(
            @RequestBody MassiveProcessingRequest request) {
        
        log.info("Starting massive account processing: {} consents with {} concurrency", 
                request.consentIds().size(), request.targetConcurrency());
        
        return accountService.processMassiveAccountWorkload(request.consentIds(), request.targetConcurrency())
                .thenApply(result -> {
                    var response = new MassiveProcessingResponse(
                            result.processedCount(),
                            result.errorCount(),
                            result.durationMs(),
                            result.strategy(),
                            request.targetConcurrency(),
                            calculateThroughput(result.processedCount(), result.durationMs()),
                            true,
                            "Massive processing completed successfully"
                    );
                    
                    log.info("Massive processing completed: {} processed, {} errors in {}ms " +
                            "({:.2f} ops/second)", 
                            result.processedCount(), result.errorCount(), result.durationMs(),
                            calculateThroughput(result.processedCount(), result.durationMs()));
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Massive processing failed: {}", e.getMessage());
                    
                    var response = new MassiveProcessingResponse(
                            0, 0, 0, "FAILED", 0, 0.0, false, e.getMessage()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Update balances for multiple accounts in parallel.
     */
    @PostMapping("/balances/update")
    @Timed(name = "accounts.balances.update", description = "Time taken to update account balances")
    public CompletableFuture<ResponseEntity<BalanceUpdateResponse>> updateAccountBalances(
            @RequestBody BalanceUpdateRequest request) {
        
        log.info("Starting balance update for {} accounts", request.accountIds().size());
        
        return accountService.updateAccountBalancesAsync(request.accountIds())
                .thenApply(result -> {
                    var response = new BalanceUpdateResponse(
                            result.updatedCount(),
                            result.errorCount(),
                            result.durationMs(),
                            true,
                            "Balance update completed successfully"
                    );
                    
                    log.info("Balance update completed: {} updated, {} errors in {}ms", 
                            result.updatedCount(), result.errorCount(), result.durationMs());
                    
                    return ResponseEntity.ok(response);
                })
                .exceptionally(e -> {
                    log.error("Balance update failed: {}", e.getMessage());
                    
                    var response = new BalanceUpdateResponse(
                            0, 0, 0, false, e.getMessage()
                    );
                    
                    return ResponseEntity.badRequest().body(response);
                });
    }
    
    /**
     * Process accounts reactively with server-sent events.
     */
    @GetMapping(value = "/process/reactive", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AccountProcessingEvent> processAccountsReactively(
            @RequestParam List<UUID> consentIds) {
        
        log.info("Starting reactive account processing for {} consents", consentIds.size());
        
        return accountService.processAccountsReactively(consentIds)
                .map(result -> new AccountProcessingEvent(
                        result.consentId(),
                        result.accountCount(),
                        result.success(),
                        result.durationMs(),
                        result.errorMessage()
                ))
                .delayElements(Duration.ofMillis(100)) // Add small delay for demonstration
                .doOnNext(event -> log.debug("Reactive processing event: {}", event))
                .doOnComplete(() -> log.info("Reactive processing completed"))
                .doOnError(error -> log.error("Reactive processing error: {}", error.getMessage()));
    }
    
    /**
     * Get account details.
     */
    @GetMapping("/{accountId}")
    @Timed(name = "accounts.get.details", description = "Time taken to get account details")
    public ResponseEntity<AccountDetailsResponse> getAccountDetails(@PathVariable String accountId) {
        try {
            log.info("Getting account details for: {}", accountId);
            
            var account = accountService.getAccountDetails(accountId);
            
            var response = new AccountDetailsResponse(
                    account.getAccountId(),
                    account.getType(),
                    account.getSubtype(),
                    account.getBrandName(),
                    account.getCompanyCnpj(),
                    account.getNumber(),
                    account.getCheckDigit(),
                    account.getAgencyNumber(),
                    account.getAgencyCheckDigit(),
                    true,
                    "Account details retrieved successfully"
            );
            
            log.info("Successfully retrieved account details for: {}", accountId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting account details for {}: {}", accountId, e.getMessage());
            
            var response = new AccountDetailsResponse(
                    accountId, null, null, null, null, null, null, null, null,
                    false, e.getMessage()
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Update single account balance.
     */
    @PostMapping("/{accountId}/balance")
    @Timed(name = "accounts.balance.update.single", description = "Time taken to update single account balance")
    public ResponseEntity<BalanceUpdateSingleResponse> updateAccountBalance(@PathVariable String accountId) {
        try {
            log.info("Updating balance for account: {}", accountId);
            
            accountService.updateAccountBalance(accountId);
            
            var response = new BalanceUpdateSingleResponse(
                    accountId,
                    true,
                    "Balance updated successfully"
            );
            
            log.info("Successfully updated balance for account: {}", accountId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error updating balance for account {}: {}", accountId, e.getMessage());
            
            var response = new BalanceUpdateSingleResponse(
                    accountId,
                    false,
                    e.getMessage()
            );
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get performance metrics.
     */
    @GetMapping("/metrics/performance")
    public ResponseEntity<PerformanceMetricsResponse> getPerformanceMetrics() {
        var report = performanceMonitor.getPerformanceReport();
        var recommendations = performanceMonitor.getRecommendations();
        
        var response = new PerformanceMetricsResponse(
                report.totalAccountsProcessed(),
                report.totalAccountsSynced(),
                report.totalBalancesUpdated(),
                report.totalAccountValidations(),
                report.totalBatchesProcessed(),
                report.totalErrors(),
                report.currentThroughput(),
                report.processingEfficiency(),
                report.activeVirtualThreads(),
                report.concurrentAccountOperations(),
                report.errorRate(),
                recommendations.recommendedBatchSize(),
                recommendations.recommendedConcurrency(),
                recommendations.optimizationSuggestions()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get resource utilization.
     */
    @GetMapping("/metrics/resources")
    public ResponseEntity<ResourceUtilizationResponse> getResourceUtilization() {
        var utilization = resourceManager.getResourceUtilization();
        
        var response = new ResourceUtilizationResponse(
                utilization.activeAccountProcessingTasks(),
                utilization.activeBalanceUpdateTasks(),
                utilization.activeValidationTasks(),
                utilization.activeApiCalls(),
                utilization.activeBatchProcessingTasks(),
                utilization.currentCpuUsage(),
                utilization.currentMemoryUsage(),
                resourceManager.getDynamicBatchSize(),
                resourceManager.getDynamicConcurrencyLevel(),
                resourceManager.getDynamicBalanceUpdateConcurrency(),
                resourceManager.getDynamicValidationConcurrency(),
                resourceManager.getDynamicApiCallConcurrency()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        var utilization = resourceManager.getResourceUtilization();
        var report = performanceMonitor.getPerformanceReport();
        
        boolean healthy = utilization.currentCpuUsage() < 0.90 
                && utilization.currentMemoryUsage() < 0.90
                && report.errorRate() < 0.20;
        
        return ResponseEntity.ok(Map.of(
                "status", healthy ? "UP" : "DOWN",
                "cpuUsage", String.format("%.2f%%", utilization.currentCpuUsage() * 100),
                "memoryUsage", String.format("%.2f%%", utilization.currentMemoryUsage() * 100),
                "errorRate", String.format("%.2f%%", report.errorRate() * 100),
                "throughput", String.format("%.2f ops/sec", report.currentThroughput()),
                "activeVirtualThreads", report.activeVirtualThreads()
        ));
    }
    
    // Helper methods
    
    private double calculateThroughput(int operations, long durationMs) {
        if (durationMs == 0) return 0.0;
        return operations * 1000.0 / durationMs;
    }
    
    // Request/Response DTOs
    
    public record BatchSyncRequest(List<UUID> consentIds) {}
    
    public record MassiveProcessingRequest(List<UUID> consentIds, int targetConcurrency) {}
    
    public record BalanceUpdateRequest(List<String> accountIds) {}
    
    public record AccountSyncResponse(
            UUID consentId,
            int accountCount,
            boolean success,
            String message,
            List<AccountSummary> accounts
    ) {}
    
    public record BatchSyncResponse(
            int totalConsents,
            int totalAccounts,
            boolean success,
            String message,
            List<AccountSummary> accounts
    ) {}
    
    public record AdaptiveProcessingResponse(
            int processedCount,
            int errorCount,
            long durationMs,
            String strategy,
            int batchSize,
            int concurrencyLevel,
            boolean success,
            String message
    ) {}
    
    public record MassiveProcessingResponse(
            int processedCount,
            int errorCount,
            long durationMs,
            String strategy,
            int targetConcurrency,
            double throughput,
            boolean success,
            String message
    ) {}
    
    public record BalanceUpdateResponse(
            int updatedCount,
            int errorCount,
            long durationMs,
            boolean success,
            String message
    ) {}
    
    public record BalanceUpdateSingleResponse(
            String accountId,
            boolean success,
            String message
    ) {}
    
    public record AccountDetailsResponse(
            String accountId,
            String type,
            String subtype,
            String brandName,
            String companyCnpj,
            String number,
            String checkDigit,
            String agencyNumber,
            String agencyCheckDigit,
            boolean success,
            String message
    ) {}
    
    public record AccountProcessingEvent(
            UUID consentId,
            int accountCount,
            boolean success,
            long durationMs,
            String errorMessage
    ) {}
    
    public record AccountSummary(
            String accountId,
            String type,
            String subtype,
            String brandName
    ) {}
    
    public record PerformanceMetricsResponse(
            long totalAccountsProcessed,
            long totalAccountsSynced,
            long totalBalancesUpdated,
            long totalAccountValidations,
            long totalBatchesProcessed,
            long totalErrors,
            double currentThroughput,
            double processingEfficiency,
            int activeVirtualThreads,
            int concurrentAccountOperations,
            double errorRate,
            int recommendedBatchSize,
            int recommendedConcurrency,
            String optimizationSuggestions
    ) {}
    
    public record ResourceUtilizationResponse(
            int activeAccountProcessingTasks,
            int activeBalanceUpdateTasks,
            int activeValidationTasks,
            int activeApiCalls,
            int activeBatchProcessingTasks,
            double currentCpuUsage,
            double currentMemoryUsage,
            int dynamicBatchSize,
            int dynamicConcurrencyLevel,
            int dynamicBalanceUpdateConcurrency,
            int dynamicValidationConcurrency,
            int dynamicApiCallConcurrency
    ) {}
}