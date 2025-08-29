package br.com.openfinance.token.client.spring;

import br.com.openfinance.token.client.TokenServiceClient;
import br.com.openfinance.token.grpc.AccessTokenResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class TokenServiceFacade {

    private final TokenServiceClient client;

    public TokenServiceFacade(TokenServiceClient client) {
        this.client = client;
    }

    public String getToken(String consentId) {
        return client.getAccessToken(consentId, "default", List.of());
    }

    public String getToken(String consentId, String organizationId) {
        return client.getAccessToken(consentId, organizationId, List.of());
    }

    public String getToken(String consentId, String organizationId, List<String> scopes) {
        return client.getAccessToken(consentId, organizationId, scopes);
    }

    public CompletableFuture<String> getTokenAsync(String consentId) {
        return client.getAccessTokenAsync(consentId, "default", List.of());
    }

    public AccessTokenResponse refreshToken(String refreshToken, String consentId) {
        return client.refreshToken(refreshToken, consentId);
    }

    public boolean isValidToken(String token) {
        return client.validateToken(token, List.of());
    }

    public boolean isValidToken(String token, List<String> requiredScopes) {
        return client.validateToken(token, requiredScopes);
    }

    public void revokeToken(String token) {
        client.revokeToken(token, "User requested");
    }

    public void revokeToken(String token, String reason) {
        client.revokeToken(token, reason);
    }
}
