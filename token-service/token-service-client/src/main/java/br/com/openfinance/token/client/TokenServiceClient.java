package br.com.openfinance.token.client;

import br.com.openfinance.token.grpc.*;
import com.google.protobuf.Empty;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TokenServiceClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TokenServiceClient.class);

    private final ManagedChannel channel;
    private final TokenServiceGrpc.TokenServiceBlockingStub blockingStub;
    private final TokenServiceGrpc.TokenServiceStub asyncStub;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TokenCache localCache;
    private final ScheduledExecutorService scheduler;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String host = "localhost";
        private int port = 9090;
        private boolean useTls = false;
        private Duration timeout = Duration.ofSeconds(10);
        private int maxRetries = 3;
        private boolean enableCache = true;
        private int cacheMaxSize = 1000;
        private Duration cacheTtl = Duration.ofMinutes(5);

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder useTls(boolean useTls) {
            this.useTls = useTls;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder enableCache(boolean enableCache) {
            this.enableCache = enableCache;
            return this;
        }

        public Builder cacheMaxSize(int cacheMaxSize) {
            this.cacheMaxSize = cacheMaxSize;
            return this;
        }

        public Builder cacheTtl(Duration cacheTtl) {
            this.cacheTtl = cacheTtl;
            return this;
        }

        public TokenServiceClient build() {
            return new TokenServiceClient(this);
        }
    }

    private TokenServiceClient(Builder builder) {
        log.info("Initializing TokenServiceClient with host: {}:{}", builder.host, builder.port);

        // Build channel
        var channelBuilder = ManagedChannelBuilder.forAddress(builder.host, builder.port);
        if (!builder.useTls) {
            channelBuilder.usePlaintext();
        }

        this.channel = channelBuilder
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .build();

        this.blockingStub = TokenServiceGrpc.newBlockingStub(channel)
                .withDeadlineAfter(builder.timeout.toMillis(), TimeUnit.MILLISECONDS);

        this.asyncStub = TokenServiceGrpc.newStub(channel);

        // Setup resilience
        this.circuitBreaker = CircuitBreaker.of("token-service",
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(10)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .build()
        );

        this.retry = Retry.of("token-service",
                RetryConfig.custom()
                        .maxAttempts(builder.maxRetries)
                        .waitDuration(Duration.ofMillis(500))
                        .retryExceptions(StatusRuntimeException.class)
                        .build()
        );

        // Setup cache
        this.localCache = builder.enableCache ?
                new TokenCache(builder.cacheMaxSize, builder.cacheTtl) : null;

        this.scheduler = Executors.newScheduledThreadPool(1);

        // Start health check
        startHealthCheck();

        log.info("TokenServiceClient initialized successfully");
    }

    public String getAccessToken(String consentId, String organizationId, List<String> scopes) {
        log.debug("Getting access token for consent: {}", consentId);

        // Check cache first
        if (localCache != null) {
            var cached = localCache.get(consentId);
            if (cached != null) {
                log.debug("Token found in cache for consent: {}", consentId);
                return cached;
            }
        }

        var request = GetAccessTokenRequest.newBuilder()
                .setConsentId(consentId)
                .setOrganizationId(organizationId)
                .addAllScopes(scopes)
                .build();

        Supplier<AccessTokenResponse> supplier = () -> blockingStub.getAccessToken(request);

        var response = executeWithResilience(supplier);

        if (response != null) {
            // Cache the token
            if (localCache != null) {
                localCache.put(consentId, response.getAccessToken(),
                        Duration.ofSeconds(response.getExpiresIn()));
            }
            return response.getAccessToken();
        }

        return null;
    }

    public CompletableFuture<String> getAccessTokenAsync(
            String consentId,
            String organizationId,
            List<String> scopes) {

        return CompletableFuture.supplyAsync(() ->
                getAccessToken(consentId, organizationId, scopes)
        );
    }

    public AccessTokenResponse refreshToken(String refreshToken, String consentId) {
        log.debug("Refreshing token for consent: {}", consentId);

        var request = RefreshAccessTokenRequest.newBuilder()
                .setRefreshToken(refreshToken)
                .setConsentId(consentId)
                .build();

        Supplier<AccessTokenResponse> supplier = () -> blockingStub.refreshAccessToken(request);

        var response = executeWithResilience(supplier);

        if (response != null && localCache != null) {
            localCache.put(consentId, response.getAccessToken(),
                    Duration.ofSeconds(response.getExpiresIn()));
        }

        return response;
    }

    public boolean validateToken(String token, List<String> requiredScopes) {
        log.debug("Validating token");

        var request = ValidateAccessTokenRequest.newBuilder()
                .setToken(token)
                .addAllRequiredScopes(requiredScopes)
                .build();

        Supplier<ValidateAccessTokenResponse> supplier = () ->
                blockingStub.validateAccessToken(request);

        var response = executeWithResilience(supplier);

        return response != null && response.getValid();
    }

    public boolean revokeToken(String token, String reason) {
        log.info("Revoking token");

        var request = RevokeAccessTokenRequest.newBuilder()
                .setToken(token)
                .setReason(reason)
                .setRevokedBy("client")
                .build();

        Supplier<RevokeAccessTokenResponse> supplier = () ->
                blockingStub.revokeAccessToken(request);

        var response = executeWithResilience(supplier);

        if (response != null && response.getSuccess()) {
            if (localCache != null) {
                localCache.evictByToken(token);
            }
            return true;
        }

        return false;
    }

    public TokenMetadata getTokenMetadata(String token) {
        log.debug("Getting token metadata");

        var request = GetTokenMetadataRequest.newBuilder()
                .setToken(token)
                .build();

        Supplier<TokenMetadata> supplier = () -> blockingStub.getTokenMetadata(request);

        return executeWithResilience(supplier);
    }

    public Map<String, AccessTokenResponse> batchGetTokens(
            List<String> consentIds,
            String organizationId) {

        log.info("Batch getting tokens for {} consents", consentIds.size());

        var request = BatchGetAccessTokensRequest.newBuilder()
                .addAllConsentIds(consentIds)
                .setOrganizationId(organizationId)
                .build();

        Supplier<BatchGetAccessTokensResponse> supplier = () ->
                blockingStub.batchGetAccessTokens(request);

        var response = executeWithResilience(supplier);

        if (response != null) {
            // Cache all tokens
            if (localCache != null) {
                response.getTokensMap().forEach((consentId, tokenResponse) ->
                        localCache.put(consentId, tokenResponse.getAccessToken(),
                                Duration.ofSeconds(tokenResponse.getExpiresIn()))
                );
            }

            return response.getTokensMap();
        }

        return Map.of();
    }

    public void subscribeToEvents(Consumer<TokenEvent> eventConsumer) {
        log.info("Subscribing to token events");

        asyncStub.streamTokenEvents(Empty.getDefaultInstance(), new StreamObserver<TokenEvent>() {
            @Override
            public void onNext(TokenEvent event) {
                try {
                    eventConsumer.accept(event);
                } catch (Exception e) {
                    log.error("Error processing token event", e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error in token event stream", t);
                // Reconnect after delay
                scheduler.schedule(() -> subscribeToEvents(eventConsumer), 5, TimeUnit.SECONDS);
            }

            @Override
            public void onCompleted() {
                log.info("Token event stream completed");
            }
        });
    }

    private <T> T executeWithResilience(Supplier<T> supplier) {
        try {
            Supplier<T> decoratedSupplier = CircuitBreaker
                    .decorateSupplier(circuitBreaker, supplier);

            decoratedSupplier = Retry
                    .decorateSupplier(retry, decoratedSupplier);

            return decoratedSupplier.get();

        } catch (Exception e) {
            log.error("Error executing gRPC call", e);
            return null;
        }
    }

    private void startHealthCheck() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                var request = ValidateAccessTokenRequest.newBuilder()
                        .setToken("health-check")
                        .build();

                blockingStub.withDeadlineAfter(1, TimeUnit.SECONDS)
                        .validateAccessToken(request);

                log.trace("Health check successful");
            } catch (Exception e) {
                log.warn("Health check failed: {}", e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        log.info("Closing TokenServiceClient");
        try {
            scheduler.shutdown();
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            channel.shutdownNow();
        }
    }
}
