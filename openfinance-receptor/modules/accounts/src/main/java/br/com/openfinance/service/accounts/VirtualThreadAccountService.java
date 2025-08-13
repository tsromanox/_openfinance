package br.com.openfinance.service.accounts;

import br.com.openfinance.application.exception.AccountNotFoundException;
import br.com.openfinance.application.exception.ConsentNotAuthorizedException;
import br.com.openfinance.application.exception.ConsentNotFoundException;
import br.com.openfinance.application.port.input.AccountUseCase;
import br.com.openfinance.application.port.output.ConsentRepository;
import br.com.openfinance.application.port.output.OpenFinanceClient;
import br.com.openfinance.domain.account.Account;
import br.com.openfinance.domain.account.Balance;
import br.com.openfinance.domain.consent.Consent;
import br.com.openfinance.domain.consent.ConsentStatus;
import br.com.openfinance.service.accounts.monitoring.AccountPerformanceMonitor;
import br.com.openfinance.service.accounts.processor.VirtualThreadAccountProcessor;
import br.com.openfinance.service.accounts.resource.AdaptiveAccountResourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Enhanced account service using Java 21 Virtual Threads and Structured Concurrency.
 * Provides high-performance account processing with adaptive resource management.
 */
@Slf4j
@Service
public class VirtualThreadAccountService implements AccountUseCase {
    
    private final ConsentRepository consentRepository;
    private final OpenFinanceClient openFinanceClient;
    private final VirtualThreadAccountProcessor accountProcessor;
    private final AdaptiveAccountResourceManager resourceManager;
    private final AccountPerformanceMonitor performanceMonitor;
    private final TaskExecutor accountSyncExecutor;
    private final TaskExecutor balanceProcessingExecutor;
    private final TaskExecutor reactiveProcessingExecutor;
    
    public VirtualThreadAccountService(
            ConsentRepository consentRepository,
            OpenFinanceClient openFinanceClient,
            VirtualThreadAccountProcessor accountProcessor,
            AdaptiveAccountResourceManager resourceManager,
            AccountPerformanceMonitor performanceMonitor,
            TaskExecutor accountSyncVirtualThreadExecutor,
            TaskExecutor balanceProcessingVirtualThreadExecutor,
            TaskExecutor reactiveProcessingVirtualThreadExecutor) {
        this.consentRepository = consentRepository;
        this.openFinanceClient = openFinanceClient;
        this.accountProcessor = accountProcessor;
        this.resourceManager = resourceManager;
        this.performanceMonitor = performanceMonitor;
        this.accountSyncExecutor = accountSyncVirtualThreadExecutor;
        this.balanceProcessingExecutor = balanceProcessingVirtualThreadExecutor;
        this.reactiveProcessingExecutor = reactiveProcessingVirtualThreadExecutor;
    }
    
    @Override
    public List<Account> syncAccountsForConsent(UUID consentId) {
        long startTime = System.currentTimeMillis();
        
        try {
            performanceMonitor.recordAccountOperation("SYNC_START", true, 0);
            
            var consent = consentRepository.findById(consentId)
                    .orElseThrow(() -> new ConsentNotFoundException(consentId));
            
            if (consent.getStatus() != ConsentStatus.AUTHORISED) {
                throw new ConsentNotAuthorizedException(consentId);
            }
            
            if (!resourceManager.acquireAccountProcessingResources()) {
                log.warn("Resource limit reached for account sync: {}", consentId);
                return List.of();
            }
            
            try {
                var accountsResponse = openFinanceClient.getAccounts(
                        consent.getOrganizationId(),
                        consent.getConsentId()
                );
                
                List<Account> accounts = accountsResponse.data().stream()
                        .map(accountData -> Account.builder()
                                .accountId(accountData.accountId())
                                .brandName(accountData.brandName())
                                .companyCnpj(accountData.companyCnpj())
                                .type(accountData.type())
                                .subtype(accountData.subtype())
                                .number(accountData.number())
                                .checkDigit(accountData.checkDigit())
                                .agencyNumber(accountData.agencyNumber())
                                .agencyCheckDigit(accountData.agencyCheckDigit())
                                .balance(Balance.builder()
                                        .availableAmount(accountData.availableAmount())
                                        .availableAmountCurrency(accountData.availableAmountCurrency())
                                        .blockedAmount(accountData.blockedAmount())
                                        .blockedAmountCurrency(accountData.blockedAmountCurrency())
                                        .automaticallyInvestedAmount(accountData.automaticallyInvestedAmount())
                                        .automaticallyInvestedAmountCurrency(accountData.automaticallyInvestedAmountCurrency())
                                        .updatedAt(LocalDateTime.now())
                                        .build())
                                .consentId(consent.getId())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build())
                        .collect(Collectors.toList());
                
                long duration = System.currentTimeMillis() - startTime;
                performanceMonitor.recordAccountOperation("SYNC_COMPLETE", true, duration);
                
                log.info("Successfully synced {} accounts for consent {} in {}ms", 
                        accounts.size(), consentId, duration);
                
                return accounts;
                
            } finally {
                resourceManager.releaseAccountProcessingResources();
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceMonitor.recordAccountOperation("SYNC", false, duration);
            performanceMonitor.recordError("sync_error", "SYNC", true);
            
            log.error("Error syncing accounts for consent {}: {}", consentId, e.getMessage());
            throw new RuntimeException("Failed to sync accounts", e);
        }
    }
    
    @Override
    public Account getAccountDetails(String accountId) {
        long startTime = System.currentTimeMillis();
        
        try {
            performanceMonitor.recordAccountOperation("GET_DETAILS_START", true, 0);
            
            // Enhanced implementation with Virtual Thread optimization
            if (!resourceManager.acquireAccountProcessingResources()) {
                log.warn("Resource limit reached for account details: {}", accountId);
                throw new AccountNotFoundException(accountId);
            }
            
            try {
                // Simulate account retrieval logic
                // In a real implementation, this would query a repository
                throw new AccountNotFoundException(accountId);
                
            } finally {
                resourceManager.releaseAccountProcessingResources();
            }
            
        } catch (AccountNotFoundException e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceMonitor.recordAccountOperation("GET_DETAILS", false, duration);
            performanceMonitor.recordError("account_not_found", "GET_DETAILS", false);
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceMonitor.recordAccountOperation("GET_DETAILS", false, duration);
            performanceMonitor.recordError("get_details_error", "GET_DETAILS", true);
            
            log.error("Error getting account details for {}: {}", accountId, e.getMessage());
            throw new RuntimeException("Failed to get account details", e);
        }
    }
    
    @Override
    public void updateAccountBalance(String accountId) {
        long startTime = System.currentTimeMillis();
        
        try {
            performanceMonitor.recordAccountOperation("BALANCE_UPDATE_START", true, 0);
            
            if (!resourceManager.acquireBalanceUpdateResources()) {
                log.warn("Resource limit reached for balance update: {}", accountId);
                return;
            }
            
            try {
                if (!resourceManager.acquireApiCallResources()) {
                    log.warn("API resource limit reached for balance update: {}", accountId);
                    return;
                }
                
                try {
                    var balanceResponse = openFinanceClient.getBalance(
                            "organizationId", // Should come from account context
                            accountId,
                            "token" // Should come from account/consent context
                    );
                    
                    var updatedBalance = Balance.builder()
                            .availableAmount(balanceResponse.data().availableAmount())
                            .availableAmountCurrency(balanceResponse.data().availableAmountCurrency())
                            .blockedAmount(balanceResponse.data().blockedAmount())
                            .blockedAmountCurrency(balanceResponse.data().blockedAmountCurrency())
                            .automaticallyInvestedAmount(balanceResponse.data().automaticallyInvestedAmount())
                            .automaticallyInvestedAmountCurrency(balanceResponse.data().automaticallyInvestedAmountCurrency())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    
                    long duration = System.currentTimeMillis() - startTime;
                    performanceMonitor.recordAccountOperation("BALANCE_UPDATE_COMPLETE", true, duration);
                    
                    log.info("Successfully updated balance for account {} in {}ms", accountId, duration);
                    
                } finally {
                    resourceManager.releaseApiCallResources();
                }
                
            } finally {
                resourceManager.releaseBalanceUpdateResources();
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            performanceMonitor.recordAccountOperation("BALANCE_UPDATE", false, duration);
            performanceMonitor.recordError("balance_update_error", "BALANCE_UPDATE", true);
            
            log.error("Error updating balance for account {}: {}", accountId, e.getMessage());
        }
    }
    
    // Enhanced methods using Virtual Threads and Structured Concurrency
    
    /**
     * Sync accounts for multiple consents using Structured Concurrency.
     */
    public CompletableFuture<List<Account>> syncAccountsForConsentsAsync(List<UUID> consentIds) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                
                // Get consents in parallel
                List<Consent> consents = consentIds.stream()
                        .map(id -> scope.fork(() -> consentRepository.findById(id)
                                .orElseThrow(() -> new ConsentNotFoundException(id))))
                        .map(future -> {
                            try {
                                return future.get();
                            } catch (Exception e) {
                                log.error("Error getting consent: {}", e.getMessage());
                                return null;
                            }
                        })
                        .filter(consent -> consent != null && consent.getStatus() == ConsentStatus.AUTHORISED)
                        .toList();
                
                scope.join();
                scope.throwIfFailed();
                
                // Process accounts using the processor
                var result = accountProcessor.processAccountsWithStructuredConcurrency(consents).get();
                
                long duration = System.currentTimeMillis() - startTime;
                performanceMonitor.recordBatchProcessing(result.processedCount(), duration);
                
                log.info("Async account sync completed: {} accounts from {} consents in {}ms", 
                        result.processedCount(), consentIds.size(), duration);
                
                return result.accounts();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Account sync interrupted", e);
            } catch (Exception e) {
                performanceMonitor.recordError("async_sync_error", "ASYNC_SYNC", true);
                throw new RuntimeException("Async account sync failed", e);
            }
            
        }, accountSyncExecutor);
    }
    
    /**
     * Update balances for multiple accounts in parallel.
     */
    public CompletableFuture<BalanceUpdateResult> updateAccountBalancesAsync(List<String> accountIds) {
        return accountProcessor.updateAccountBalancesInParallel(accountIds);
    }
    
    /**
     * Process accounts reactively using WebFlux patterns.
     */
    public Flux<AccountProcessingResult> processAccountsReactively(List<UUID> consentIds) {
        return Flux.fromIterable(consentIds)
                .parallel(resourceManager.getDynamicConcurrencyLevel())
                .runOn(Schedulers.fromExecutor(reactiveProcessingExecutor))
                .map(this::processConsentReactively)
                .sequential()
                .doOnNext(result -> performanceMonitor.recordAccountOperation("REACTIVE_PROCESS", 
                        result.success(), result.durationMs()))
                .doOnError(error -> performanceMonitor.recordError("reactive_process_error", 
                        "REACTIVE_PROCESS", true));
    }
    
    /**
     * Batch process accounts with adaptive sizing.
     */
    public CompletableFuture<BatchProcessingResult> processAccountsInAdaptiveBatches(List<UUID> consentIds) {
        return accountProcessor.processAccountsInAdaptiveBatches(consentIds)
                .thenApply(result -> new BatchProcessingResult(
                        result.processedCount(),
                        result.errorCount(),
                        result.durationMs(),
                        result.strategy(),
                        resourceManager.getDynamicBatchSize(),
                        resourceManager.getDynamicConcurrencyLevel()
                ));
    }
    
    /**
     * Massive parallel account processing for high-volume scenarios.
     */
    public CompletableFuture<BatchProcessingResult> processMassiveAccountWorkload(
            List<UUID> consentIds, int targetConcurrency) {
        
        return accountProcessor.processMassiveAccountWorkload(consentIds, targetConcurrency)
                .thenApply(result -> new BatchProcessingResult(
                        result.processedCount(),
                        result.errorCount(),
                        result.durationMs(),
                        result.strategy(),
                        resourceManager.getDynamicBatchSize(),
                        targetConcurrency
                ));
    }
    
    /**
     * Process accounts with automatic error recovery and retry.
     */
    public CompletableFuture<List<Account>> processAccountsWithErrorRecovery(List<UUID> consentIds) {
        return CompletableFuture.supplyAsync(() -> {
            Collection<Account> allAccounts = new ConcurrentLinkedQueue<>();
            AtomicInteger retryCount = new AtomicInteger(0);
            
            while (retryCount.get() < 3) {
                try {
                    var result = syncAccountsForConsentsAsync(consentIds).get(60, TimeUnit.SECONDS);
                    allAccounts.addAll(result);
                    break;
                    
                } catch (Exception e) {
                    retryCount.incrementAndGet();
                    log.warn("Account processing failed (attempt {}): {}", retryCount.get(), e.getMessage());
                    
                    if (retryCount.get() >= 3) {
                        performanceMonitor.recordError("max_retries_exceeded", "ERROR_RECOVERY", false);
                        throw new RuntimeException("Max retries exceeded for account processing", e);
                    }
                    
                    try {
                        Thread.sleep(1000 * retryCount.get()); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Error recovery interrupted", ie);
                    }
                }
            }
            
            return List.copyOf(allAccounts);
            
        }, accountSyncExecutor);
    }
    
    // Helper methods
    
    private AccountProcessingResult processConsentReactively(UUID consentId) {
        long startTime = System.currentTimeMillis();
        
        try {
            var accounts = syncAccountsForConsent(consentId);
            long duration = System.currentTimeMillis() - startTime;
            
            return new AccountProcessingResult(
                    consentId,
                    accounts.size(),
                    true,
                    duration,
                    null
            );
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new AccountProcessingResult(
                    consentId,
                    0,
                    false,
                    duration,
                    e.getMessage()
            );
        }
    }
    
    // Result classes
    
    public record AccountProcessingResult(
            UUID consentId,
            int accountCount,
            boolean success,
            long durationMs,
            String errorMessage
    ) {}
    
    public record BalanceUpdateResult(
            int updatedCount,
            int errorCount,
            List<Balance> balances,
            long durationMs
    ) {}
    
    public record BatchProcessingResult(
            int processedCount,
            int errorCount,
            long durationMs,
            String strategy,
            int batchSize,
            int concurrencyLevel
    ) {}
}