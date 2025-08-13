package br.com.openfinance.infrastructure.client;

import br.com.openfinance.application.dto.AccountsResponse;
import br.com.openfinance.application.dto.BalanceResponse;
import br.com.openfinance.infrastructure.client.auth.OAuth2ClientService;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Performance and integration tests for VirtualThreadOpenFinanceClient.
 * Tests parallel processing capabilities and Virtual Threads optimization.
 */
@ExtendWith(MockitoExtension.class)
class VirtualThreadOpenFinanceClientTest {
    
    private WireMockServer wireMockServer;
    private VirtualThreadOpenFinanceClient client;
    private SimpleMeterRegistry meterRegistry;
    
    @Mock
    private OAuth2ClientService oauth2Service;
    
    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        
        // Configure OAuth2 mock
        OAuth2ClientService.AccessToken mockToken = new OAuth2ClientService.AccessToken(
                "mock-token", "Bearer", 
                java.time.Instant.now().plus(Duration.ofHours(1)), 
                "accounts"
        );
        when(oauth2Service.getAccessToken(anyString())).thenReturn(mockToken);
        
        // Initialize client with test configuration
        meterRegistry = new SimpleMeterRegistry();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
        Retry retry = Retry.of("test", RetryConfig.ofDefaults());
        
        client = new VirtualThreadOpenFinanceClient(oauth2Service, circuitBreaker, retry, meterRegistry);
        
        // Set test base URL
        ReflectionTestUtils.setField(client, "baseUrl", wireMockServer.baseUrl());
        ReflectionTestUtils.setField(client, "maxConcurrentRequests", 100);
    }
    
    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
    
    @Test
    void shouldHandleHighConcurrencyWithVirtualThreads() throws InterruptedException {
        // Setup mock responses
        setupBalanceEndpointMock();
        
        int numberOfAccounts = 1000;
        List<String> accountIds = IntStream.range(1, numberOfAccounts + 1)
                .mapToObj(i -> "account-" + i)
                .toList();
        
        long startTime = System.currentTimeMillis();
        
        // Execute parallel processing
        List<BalanceResponse> results = client.getBalancesParallel("org-1", accountIds, "test-token");
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Assertions
        assertThat(results).hasSize(numberOfAccounts);
        assertThat(duration).isLessThan(5000); // Should complete in under 5 seconds
        
        // Verify metrics
        assertThat(meterRegistry.find("openfinance.restclient.requests").counter().count())
                .isGreaterThan(0);
        
        System.out.printf("Processed %d accounts in %d ms (%.2f accounts/second)%n", 
                numberOfAccounts, duration, numberOfAccounts * 1000.0 / duration);
    }
    
    @Test
    void shouldMaintainPerformanceUnderLoad() throws InterruptedException {
        setupAccountsEndpointMock();
        
        int numberOfOrgs = 50;
        int concurrentThreads = 10;
        CountDownLatch latch = new CountDownLatch(concurrentThreads);
        
        List<String> orgIds = IntStream.range(1, numberOfOrgs + 1)
                .mapToObj(i -> "org-" + i)
                .toList();
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        long startTime = System.currentTimeMillis();
        
        // Execute concurrent load
        for (int i = 0; i < concurrentThreads; i++) {
            executor.submit(() -> {
                try {
                    client.getAccountsForOrganizationsAsync(orgIds, "test-token").get();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for completion
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        executor.shutdown();
        
        // Assertions
        assertThat(completed).isTrue();
        assertThat(duration).isLessThan(15000); // Should complete in under 15 seconds
        
        System.out.printf("Processed %d concurrent loads in %d ms%n", concurrentThreads, duration);
    }
    
    @Test
    void shouldAdaptBatchSizeBasedOnPerformance() {
        setupBalanceEndpointMock();
        
        List<String> accountIds = IntStream.range(1, 201)
                .mapToObj(i -> "account-" + i)
                .toList();
        
        long startTime = System.currentTimeMillis();
        
        // Test adaptive batch processing
        List<BalanceResponse> results = client.processBatchAdaptive(
                accountIds,
                accountId -> client.getBalance("org-1", accountId, "test-token"),
                20 // Initial batch size
        );
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(results).hasSize(200);
        assertThat(duration).isLessThan(10000);
        
        System.out.printf("Adaptive processing completed %d items in %d ms%n", results.size(), duration);
    }
    
    @Test
    void shouldProvideAccuratePerformanceStats() throws InterruptedException {
        setupBalanceEndpointMock();
        
        // Execute some operations
        client.getBalance("org-1", "account-1", "test-token");
        client.getBalance("org-1", "account-2", "test-token");
        
        Thread.sleep(100); // Allow metrics to update
        
        VirtualThreadOpenFinanceClient.PerformanceStats stats = client.getPerformanceStats();
        
        assertThat(stats.totalRequests()).isGreaterThan(0);
        assertThat(stats.totalErrors()).isEqualTo(0);
        assertThat(stats.averageRequestTimeSeconds()).isGreaterThan(0);
        assertThat(stats.circuitBreakerState()).isEqualTo("CLOSED");
        
        System.out.printf("Performance Stats: Requests=%d, Errors=%d, AvgTime=%.3fs, CB=%s%n",
                stats.totalRequests(), stats.totalErrors(), 
                stats.averageRequestTimeSeconds(), stats.circuitBreakerState());
    }
    
    private void setupBalanceEndpointMock() {
        wireMockServer.stubFor(get(urlMatching("/open-banking/accounts/v2/accounts/[^/]+/balances"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "availableAmount": 1000.00,
                                    "availableAmountCurrency": "BRL",
                                    "blockedAmount": 0.00,
                                    "blockedAmountCurrency": "BRL",
                                    "automaticallyInvestedAmount": 0.00,
                                    "automaticallyInvestedAmountCurrency": "BRL"
                                  },
                                  "links": {
                                    "self": "https://api.example.com/accounts/balance"
                                  },
                                  "meta": {
                                    "requestDateTime": "2024-01-15T10:00:00Z"
                                  }
                                }
                                """)
                        .withFixedDelay(50))); // Simulate network latency
    }
    
    private void setupAccountsEndpointMock() {
        wireMockServer.stubFor(get(urlEqualTo("/open-banking/accounts/v2/accounts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": [
                                    {
                                      "accountId": "account-1",
                                      "brandName": "Test Bank",
                                      "companyCnpj": "12345678000100",
                                      "type": "CONTA_DEPOSITO_A_VISTA",
                                      "subtype": "INDIVIDUAL",
                                      "number": "12345",
                                      "checkDigit": "6",
                                      "agencyNumber": "0001",
                                      "agencyCheckDigit": "9",
                                      "availableAmount": 1000.00,
                                      "availableAmountCurrency": "BRL",
                                      "blockedAmount": 0.00,
                                      "blockedAmountCurrency": "BRL",
                                      "automaticallyInvestedAmount": 0.00,
                                      "automaticallyInvestedAmountCurrency": "BRL"
                                    }
                                  ],
                                  "links": {
                                    "self": "https://api.example.com/accounts"
                                  },
                                  "meta": {
                                    "totalRecords": 1,
                                    "totalPages": 1,
                                    "requestDateTime": "2024-01-15T10:00:00Z"
                                  }
                                }
                                """)
                        .withFixedDelay(100)));
    }
}