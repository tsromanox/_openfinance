package br.com.openfinance.token.server.service;

import br.com.openfinance.token.grpc.*;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.annotation.Timed;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementação do serviço gRPC de gerenciamento de tokens
 */
@GrpcService
public class TokenServiceImpl extends TokenServiceGrpc.TokenServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(TokenServiceImpl.class);

    private final TokenManager tokenManager;
    private final TokenCache tokenCache;
    private final TokenEventPublisher eventPublisher;
    private final Map<String, StreamObserver<TokenEvent>> eventObservers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Autowired
    public TokenServiceImpl(
            TokenManager tokenManager,
            TokenCache tokenCache,
            TokenEventPublisher eventPublisher) {
        this.tokenManager = tokenManager;
        this.tokenCache = tokenCache;
        this.eventPublisher = eventPublisher;

        // Start event broadcasting
        startEventBroadcaster();
    }

    @Override
    @Timed(value = "grpc.token.get")
    public void getAccessToken(
            GetAccessTokenRequest request,
            StreamObserver<AccessTokenResponse> responseObserver) {

        log.debug("Getting access token for consent: {}", request.getConsentId());

        try {
            // Check cache first
            var cachedToken = tokenCache.get(request.getConsentId());
            if (cachedToken != null && !tokenManager.isExpired(cachedToken)) {
                responseObserver.onNext(cachedToken);
                responseObserver.onCompleted();
                return;
            }

            // Generate new token
            var token = tokenManager.generateToken(
                    request.getConsentId(),
                    request.getOrganizationId(),
                    request.getScopesList(),
                    request.getMetadataMap()
            );

            // Cache it
            tokenCache.put(request.getConsentId(), token);

            // Publish event
            publishTokenEvent(TokenEventType.TOKEN_EVENT_CREATED, token);

            // Send response
            responseObserver.onNext(token);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error generating access token", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to generate access token")
                            .withCause(e)
                            .asException()
            );
        }
    }

    @Override
    @Timed(value = "grpc.token.refresh")
    public void refreshAccessToken(
            RefreshAccessTokenRequest request,
            StreamObserver<AccessTokenResponse> responseObserver) {

        log.debug("Refreshing token for consent: {}", request.getConsentId());

        try {
            var newToken = tokenManager.refreshToken(
                    request.getRefreshToken(),
                    request.getConsentId()
            );

            // Update cache
            tokenCache.put(request.getConsentId(), newToken);

            // Publish event
            publishTokenEvent(TokenEventType.TOKEN_EVENT_REFRESHED, newToken);

            responseObserver.onNext(newToken);
            responseObserver.onCompleted();

        } catch (InvalidTokenException e) {
            log.warn("Invalid refresh token", e);
            responseObserver.onError(
                    io.grpc.Status.UNAUTHENTICATED
                            .withDescription("Invalid refresh token")
                            .asException()
            );
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to refresh token")
                            .asException()
            );
        }
    }

    @Override
    @Timed(value = "grpc.token.revoke")
    public void revokeAccessToken(
            RevokeAccessTokenRequest request,
            StreamObserver<RevokeAccessTokenResponse> responseObserver) {

        log.info("Revoking token: {}", request.getToken().substring(0, 10) + "...");

        try {
            var success = tokenManager.revokeToken(
                    request.getToken(),
                    request.getReason(),
                    request.getRevokedBy()
            );

            if (success) {
                // Remove from cache
                tokenCache.evictByToken(request.getToken());

                // Publish event
                publishTokenEvent(TokenEventType.TOKEN_EVENT_REVOKED, request.getToken());
            }

            var response = RevokeAccessTokenResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage(success ? "Token revoked successfully" : "Failed to revoke token")
                    .setRevokedAt(nowTimestamp())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error revoking token", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to revoke token")
                            .asException()
            );
        }
    }

    @Override
    @Timed(value = "grpc.token.validate")
    public void validateAccessToken(
            ValidateAccessTokenRequest request,
            StreamObserver<ValidateAccessTokenResponse> responseObserver) {

        log.debug("Validating token");

        try {
            var validationResult = tokenManager.validateToken(
                    request.getToken(),
                    request.getRequiredScopesList()
            );

            var response = ValidateAccessTokenResponse.newBuilder()
                    .setValid(validationResult.isValid())
                    .setStatus(validationResult.getStatus())
                    .setMessage(validationResult.getMessage())
                    .setMetadata(validationResult.getMetadata())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error validating token", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to validate token")
                            .asException()
            );
        }
    }

    @Override
    public void getTokenMetadata(
            GetTokenMetadataRequest request,
            StreamObserver<TokenMetadata> responseObserver) {

        log.debug("Getting token metadata");

        try {
            var metadata = tokenManager.getTokenMetadata(request.getToken());

            if (metadata != null) {
                responseObserver.onNext(metadata);
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(
                        io.grpc.Status.NOT_FOUND
                                .withDescription("Token not found")
                                .asException()
                );
            }

        } catch (Exception e) {
            log.error("Error getting token metadata", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to get token metadata")
                            .asException()
            );
        }
    }

    @Override
    public void streamTokenEvents(
            Empty request,
            StreamObserver<TokenEvent> responseObserver) {

        log.info("Client connected for token event streaming");

        String observerId = java.util.UUID.randomUUID().toString();
        eventObservers.put(observerId, responseObserver);

        // Handle client disconnection
        responseObserver = new DelegatingStreamObserver<>(responseObserver) {
            @Override
            public void onError(Throwable t) {
                eventObservers.remove(observerId);
                super.onError(t);
            }

            @Override
            public void onCompleted() {
                eventObservers.remove(observerId);
                super.onCompleted();
            }
        };
    }

    @Override
    public void batchGetAccessTokens(
            BatchGetAccessTokensRequest request,
            StreamObserver<BatchGetAccessTokensResponse> responseObserver) {

        log.debug("Batch getting tokens for {} consents", request.getConsentIdsCount());

        try {
            var responseBuilder = BatchGetAccessTokensResponse.newBuilder();

            for (String consentId : request.getConsentIdsList()) {
                try {
                    var token = tokenManager.generateToken(
                            consentId,
                            request.getOrganizationId(),
                            List.of(),
                            Map.of()
                    );
                    responseBuilder.putTokens(consentId, token);
                } catch (Exception e) {
                    var error = TokenError.newBuilder()
                            .setConsentId(consentId)
                            .setErrorCode("TOKEN_GENERATION_ERROR")
                            .setErrorMessage(e.getMessage())
                            .build();
                    responseBuilder.addErrors(error);
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error in batch token generation", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Failed to generate batch tokens")
                            .asException()
            );
        }
    }

    // Helper methods
    private void publishTokenEvent(TokenEventType type, Object tokenOrResponse) {
        var event = TokenEvent.newBuilder()
                .setEventId(java.util.UUID.randomUUID().toString())
                .setEventType(type)
                .setTimestamp(nowTimestamp())
                .build();

        eventPublisher.publish(event);
        broadcastEvent(event);
    }

    private void broadcastEvent(TokenEvent event) {
        eventObservers.values().parallelStream().forEach(observer -> {
            try {
                observer.onNext(event);
            } catch (Exception e) {
                log.warn("Error broadcasting event to observer", e);
            }
        });
    }

    private void startEventBroadcaster() {
        scheduler.scheduleAtFixedRate(() -> {
            // Heartbeat event
            var heartbeat = TokenEvent.newBuilder()
                    .setEventId("heartbeat")
                    .setEventType(TokenEventType.TOKEN_EVENT_UNKNOWN)
                    .setTimestamp(nowTimestamp())
                    .build();

            broadcastEvent(heartbeat);
        }, 30, 30, TimeUnit.SECONDS);
    }

    private Timestamp nowTimestamp() {
        var now = Instant.now();
        return Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }

    // Helper class for stream observer delegation
    private static class DelegatingStreamObserver<T> implements StreamObserver<T> {
        private final StreamObserver<T> delegate;

        public DelegatingStreamObserver(StreamObserver<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onNext(T value) {
            delegate.onNext(value);
        }

        @Override
        public void onError(Throwable t) {
            delegate.onError(t);
        }

        @Override
        public void onCompleted() {
            delegate.onCompleted();
        }
    }
}
