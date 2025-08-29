package com.bank.openfinance.client.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@ConfigurationProperties(prefix = "openfinance.client")
public class ClientConfiguration {

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    private String scope = "accounts openid";

    @NotNull
    private ClientType type = ClientType.IMPERATIVE;

    @Positive
    private int timeout = 30;

    @Positive
    private int maxConnections = 10000;

    @Positive
    private int maxConnectionsPerRoute = 2000;

    @Positive
    private int connectionTimeout = 10;

    @Positive
    private int socketTimeout = 30;

    private RetryConfig retry = RetryConfig.builder().build();

    private CircuitBreakerConfig circuitBreaker = CircuitBreakerConfig.builder().build();

    public enum ClientType {
        REACTIVE, IMPERATIVE
    }

    @Data
    @Builder
    public static class RetryConfig {
        @Builder.Default
        private int maxAttempts = 3;

        @Builder.Default
        private long waitDuration = 1000;

        @Builder.Default
        private double multiplier = 2.0;

        @Builder.Default
        private long maxWaitDuration = 10000;
    }

    @Data
    @Builder
    public static class CircuitBreakerConfig {
        @Builder.Default
        private float failureRateThreshold = 50.0f;

        @Builder.Default
        private int slidingWindowSize = 100;

        @Builder.Default
        private int permittedCallsInHalfOpen = 10;

        @Builder.Default
        private long waitDurationInOpenState = 30000;

        @Builder.Default
        private int minimumNumberOfCalls = 10;
    }
}
