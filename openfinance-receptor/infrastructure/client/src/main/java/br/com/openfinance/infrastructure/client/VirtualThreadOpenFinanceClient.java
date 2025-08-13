package br.com.openfinance.infrastructure.client;

import br.com.openfinance.application.dto.AccountsResponse;
import br.com.openfinance.application.dto.BalanceResponse;
import br.com.openfinance.application.dto.ConsentRequest;
import br.com.openfinance.application.dto.ConsentResponse;
import br.com.openfinance.application.port.output.OpenFinanceClient;
import br.com.openfinance.infrastructure.client.auth.OAuth2ClientService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Collectors;

/**
 * High-performance OpenFinance client using RestClient with Virtual Threads optimization.
 * This implementation is specifically designed for maximum parallelism with Virtual Threads.
 */
@Slf4j
@Component("virtualThreadOpenFinanceClient")
public class VirtualThreadOpenFinanceClient implements OpenFinanceClient {
    
    private final RestClient restClient;
    private final OAuth2ClientService oauth2Service;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Executor virtualThreadExecutor;
    private final Semaphore concurrencyLimiter;
    private final MeterRegistry meterRegistry;
    
    // Configuration
    @Value("${openfinance.client.base-url:https://api.openfinance.org.br}")
    private String baseUrl;
    
    @Value("${openfinance.client.max-concurrent-requests:1000}")
    private int maxConcurrentRequests;
    
    // Metrics
    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Timer requestTimer;
    
    public VirtualThreadOpenFinanceClient(
            OAuth2ClientService oauth2Service,
            CircuitBreaker circuitBreaker,
            Retry retry,
            MeterRegistry meterRegistry) {
        
        this.oauth2Service = oauth2Service;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.meterRegistry = meterRegistry;
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.concurrencyLimiter = new Semaphore(maxConcurrentRequests);
        
        // Configure HTTP client with Virtual Threads
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(virtualThreadExecutor)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        
        // Configure RestClient with optimized settings
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        
        // Initialize metrics
        this.requestCounter = Counter.builder("openfinance.restclient.requests")
                .description("Total OpenFinance requests via RestClient")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("openfinance.restclient.errors")
                .description("Total OpenFinance errors via RestClient")
                .register(meterRegistry);
        this.requestTimer = Timer.builder("openfinance.restclient.request.time")
                .description("OpenFinance request execution time via RestClient")
                .register(meterRegistry);
    }
    
    @Override
    public ConsentResponse createConsent(String orgId, ConsentRequest request) {
        return executeWithResilience(() -> {
            String token = oauth2Service.getAccessToken(orgId).token();
            
            return restClient.post()
                    .uri("/open-banking/consents/v3/consents")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                    .header("x-fapi-auth-date", java.time.Instant.now().toString())
                    .body(request)
                    .retrieve()
                    .body(ConsentResponse.class);
        }, "createConsent");
    }
    
    @Override
    public ConsentResponse getConsent(String orgId, String consentId) {
        return executeWithResilience(() -> {
            String token = oauth2Service.getAccessToken(orgId).token();
            
            return restClient.get()
                    .uri("/open-banking/consents/v3/consents/{consentId}", consentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                    .retrieve()
                    .body(ConsentResponse.class);
        }, "getConsent");
    }
    
    @Override
    public AccountsResponse getAccounts(String orgId, String token) {
        return executeWithResilience(() -> {
            return restClient.get()
                    .uri("/open-banking/accounts/v2/accounts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                    .retrieve()
                    .body(AccountsResponse.class);
        }, "getAccounts");
    }
    
    @Override
    public BalanceResponse getBalance(String orgId, String accountId, String token) {
        return executeWithResilience(() -> {
            return restClient.get()
                    .uri("/open-banking/accounts/v2/accounts/{accountId}/balances", accountId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("x-fapi-interaction-id", UUID.randomUUID().toString())
                    .retrieve()
                    .body(BalanceResponse.class);
        }, "getBalance");
    }
    
    /**
     * High-performance parallel processing using Virtual Threads and StructuredTaskScope.
     */
    public List<BalanceResponse> getBalancesParallel(String orgId, List<String> accountIds, String token) 
            throws InterruptedException {
        
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            // Fork tasks for each account ID
            List<StructuredTaskScope.Subtask<BalanceResponse>> subtasks = accountIds.stream()
                    .map(accountId -> scope.fork(() -> {
                        try {
                            concurrencyLimiter.acquire();
                            return getBalance(orgId, accountId, token);
                        } finally {
                            concurrencyLimiter.release();
                        }
                    }))
                    .toList();
            
            // Wait for all tasks to complete
            scope.join();
            scope.throwIfFailed();
            
            // Collect results
            return subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .collect(Collectors.toList());
                    
        } finally {
            sample.stop(Timer.builder("openfinance.batch.balances")
                    .description("Batch balance retrieval time")
                    .register(meterRegistry));
        }
    }
    
    /**
     * CompletableFuture-based parallel processing for compatibility.
     */
    public CompletableFuture<List<AccountsResponse>> getAccountsForOrganizationsAsync(
            List<String> orgIds, String token) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<AccountsResponse>> futures = orgIds.stream()
                    .map(orgId -> CompletableFuture.supplyAsync(() -> {
                        try {
                            concurrencyLimiter.acquire();
                            return getAccounts(orgId, token);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while acquiring permit", e);
                        } finally {
                            concurrencyLimiter.release();
                        }
                    }, virtualThreadExecutor))
                    .toList();
            
            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
                    
        }, virtualThreadExecutor);
    }
    
    /**
     * Batch consent processing with controlled parallelism.
     */
    public List<ConsentResponse> getConsentsParallel(String orgId, List<String> consentIds) 
            throws InterruptedException {
        
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<ConsentResponse>> subtasks = consentIds.stream()
                    .map(consentId -> scope.fork(() -> {
                        try {
                            concurrencyLimiter.acquire();
                            return getConsent(orgId, consentId);
                        } finally {
                            concurrencyLimiter.release();
                        }
                    }))
                    .toList();
            
            scope.join();
            scope.throwIfFailed();
            
            return subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .collect(Collectors.toList());
                    
        } finally {
            sample.stop(Timer.builder("openfinance.batch.consents")
                    .description("Batch consent retrieval time")
                    .register(meterRegistry));
        }
    }
    
    /**
     * Adaptive batch processing that adjusts batch size based on performance.
     */
    public <T, R> List<R> processBatchAdaptive(
            List<T> items, 
            java.util.function.Function<T, R> processor,
            int initialBatchSize) {
        
        List<R> results = new java.util.ArrayList<>();
        int batchSize = initialBatchSize;
        long lastProcessingTime = 0;
        
        for (int i = 0; i < items.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, items.size());
            List<T> batch = items.subList(i, endIndex);
            
            long startTime = System.currentTimeMillis();
            
            try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                List<StructuredTaskScope.Subtask<R>> subtasks = batch.stream()
                        .map(item -> scope.fork(() -> {
                            try {
                                concurrencyLimiter.acquire();
                                return processor.apply(item);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException("Processing interrupted", e);
                            } finally {
                                concurrencyLimiter.release();
                            }
                        }))
                        .toList();
                
                scope.join();
                scope.throwIfFailed();
                
                List<R> batchResults = subtasks.stream()
                        .map(StructuredTaskScope.Subtask::get)
                        .collect(Collectors.toList());
                
                results.addAll(batchResults);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Batch processing interrupted", e);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Adaptive batch size adjustment
            if (lastProcessingTime > 0) {
                double performanceRatio = (double) processingTime / lastProcessingTime;
                if (performanceRatio > 1.5 && batchSize > 10) {
                    batchSize = Math.max(10, batchSize - 10); // Decrease batch size
                } else if (performanceRatio < 0.8 && batchSize < 100) {
                    batchSize = Math.min(100, batchSize + 10); // Increase batch size
                }
            }
            
            lastProcessingTime = processingTime;
            
            log.debug("Processed batch of {} items in {}ms, next batch size: {}", 
                     batch.size(), processingTime, batchSize);
        }
        
        return results;
    }
    
    private <T> T executeWithResilience(java.util.function.Supplier<T> operation, String operationName) {
        Timer.Sample sample = Timer.Sample.start(meterRegistry);
        
        try {
            requestCounter.increment();
            
            return circuitBreaker.executeSupplier(() -> 
                retry.executeSupplier(operation)
            );
            
        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error executing operation {}: {}", operationName, e.getMessage());
            throw new RestClientException("Failed to execute " + operationName, e);
            
        } finally {
            sample.stop(requestTimer);
        }
    }
    
    /**
     * Get performance statistics.
     */
    public PerformanceStats getPerformanceStats() {
        return new PerformanceStats(
                (long) requestCounter.count(),
                (long) errorCounter.count(),
                concurrencyLimiter.availablePermits(),
                requestTimer.mean(java.util.concurrent.TimeUnit.SECONDS),
                circuitBreaker.getState().name()
        );
    }
    
    public record PerformanceStats(
            long totalRequests,
            long totalErrors,
            int availablePermits,
            double averageRequestTimeSeconds,
            String circuitBreakerState
    ) {}
}