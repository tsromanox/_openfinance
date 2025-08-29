package br.com.openfinance.token.server.service;

import br.com.openfinance.token.grpc.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

@Service
public class TokenManager {

    @Value("${token.issuer:https://auth.openfinance.com.br}")
    private String issuer;

    @Value("${token.expiry.access:900}") // 15 minutes
    private int accessTokenExpiry;

    @Value("${token.expiry.refresh:86400}") // 24 hours
    private int refreshTokenExpiry;

    private final KeyPair keyPair;
    private final JWSSigner signer;
    private final JWSVerifier verifier;

    public TokenManager() throws Exception {
        // Generate RSA key pair (in production, load from secure storage)
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(2048);
        this.keyPair = keyGenerator.generateKeyPair();

        this.signer = new RSASSASigner((RSAPrivateKey) keyPair.getPrivate());
        this.verifier = new RSASSAVerifier((RSAPublicKey) keyPair.getPublic());
    }

    public AccessTokenResponse generateToken(
            String consentId,
            String organizationId,
            List<String> scopes,
            Map<String, String> metadata) throws JOSEException {

        var now = Instant.now();
        var expiresAt = now.plusSeconds(accessTokenExpiry);

        // Create JWT claims
        var claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(consentId)
                .audience(organizationId)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .jwtID(UUID.randomUUID().toString())
                .claim("consent_id", consentId)
                .claim("organization_id", organizationId)
                .claim("scopes", scopes);

        // Add metadata
        metadata.forEach(claimsBuilder::claim);

        var claims = claimsBuilder.build();

        // Create and sign JWT
        var signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(UUID.randomUUID().toString())
                        .build(),
                claims
        );

        signedJWT.sign(signer);

        var accessToken = signedJWT.serialize();
        var refreshToken = generateRefreshToken(consentId);

        return AccessTokenResponse.newBuilder()
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setTokenType("Bearer")
                .setExpiresIn(accessTokenExpiry)
                .addAllScopes(scopes)
                .setCreatedAt(toTimestamp(now))
                .setExpiresAt(toTimestamp(expiresAt))
                .setConsentId(consentId)
                .build();
    }

    public AccessTokenResponse refreshToken(String refreshToken, String consentId)
            throws Exception {

        // Validate refresh token
        if (!validateRefreshToken(refreshToken, consentId)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        // Generate new access token
        return generateToken(consentId, "", List.of(), Map.of());
    }

    public boolean revokeToken(String token, String reason, String revokedBy) {
        // Store revocation in database/cache
        // Implementation depends on persistence layer
        return true;
    }

    public ValidationResult validateToken(String token, List<String> requiredScopes) {
        try {
            var jwt = SignedJWT.parse(token);

            // Verify signature
            if (!jwt.verify(verifier)) {
                return ValidationResult.invalid("Invalid signature");
            }

            var claims = jwt.getJWTClaimsSet();

            // Check expiration
            if (claims.getExpirationTime().before(new Date())) {
                return ValidationResult.expired();
            }

            // Check scopes
            @SuppressWarnings("unchecked")
            var tokenScopes = (List<String>) claims.getClaim("scopes");
            if (!tokenScopes.containsAll(requiredScopes)) {
                return ValidationResult.invalid("Insufficient scopes");
            }

            return ValidationResult.valid(buildMetadata(claims));

        } catch (Exception e) {
            return ValidationResult.invalid(e.getMessage());
        }
    }

    public TokenMetadata getTokenMetadata(String token) {
        try {
            var jwt = SignedJWT.parse(token);
            var claims = jwt.getJWTClaimsSet();

            return buildMetadata(claims);

        } catch (Exception e) {
            return null;
        }
    }

    public boolean isExpired(AccessTokenResponse token) {
        var now = Instant.now();
        var expiresAt = Instant.ofEpochSecond(
                token.getExpiresAt().getSeconds(),
                token.getExpiresAt().getNanos()
        );
        return now.isAfter(expiresAt);
    }

    private String generateRefreshToken(String consentId) {
        return Base64.getEncoder().encodeToString(
                (consentId + ":" + UUID.randomUUID()).getBytes()
        );
    }

    private boolean validateRefreshToken(String refreshToken, String consentId) {
        try {
            var decoded = new String(Base64.getDecoder().decode(refreshToken));
            return decoded.startsWith(consentId + ":");
        } catch (Exception e) {
            return false;
        }
    }

    private TokenMetadata buildMetadata(JWTClaimsSet claims) {
        return TokenMetadata.newBuilder()
                .setTokenId(claims.getJWTID())
                .setConsentId(claims.getSubject())
                .setOrganizationId(claims.getAudience().get(0))
                .setStatus(TokenStatus.TOKEN_STATUS_ACTIVE)
                .setCreatedAt(toTimestamp(claims.getIssueTime().toInstant()))
                .setExpiresAt(toTimestamp(claims.getExpirationTime().toInstant()))
                .build();
    }

    private com.google.protobuf.Timestamp toTimestamp(Instant instant) {
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    // Helper classes
    public static class ValidationResult {
        private final boolean valid;
        private final TokenStatus status;
        private final String message;
        private final TokenMetadata metadata;

        private ValidationResult(boolean valid, TokenStatus status, String message, TokenMetadata metadata) {
            this.valid = valid;
            this.status = status;
            this.message = message;
            this.metadata = metadata;
        }

        public static ValidationResult valid(TokenMetadata metadata) {
            return new ValidationResult(true, TokenStatus.TOKEN_STATUS_ACTIVE, "Token is valid", metadata);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, TokenStatus.TOKEN_STATUS_INVALID, message, null);
        }

        public static ValidationResult expired() {
            return new ValidationResult(false, TokenStatus.TOKEN_STATUS_EXPIRED, "Token expired", null);
        }

        // Getters
        public boolean isValid() { return valid; }
        public TokenStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public TokenMetadata getMetadata() { return metadata; }
    }

    public static class InvalidTokenException extends Exception {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
