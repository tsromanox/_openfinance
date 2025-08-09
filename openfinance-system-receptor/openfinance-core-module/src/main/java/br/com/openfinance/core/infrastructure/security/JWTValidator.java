package br.com.openfinance.core.infrastructure.security;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

@Component
@Slf4j
public class JWTValidator {

    @Value("${security.oauth2.resource.jwk.key-set-uri}")
    private String jwkSetUri;

    private final JWKSProvider jwksProvider;

    public JWTValidator(JWKSProvider jwksProvider) {
        this.jwksProvider = jwksProvider;
    }

    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Get public key from JWKS endpoint
            RSAPublicKey publicKey = jwksProvider.getPublicKey(
                    signedJWT.getHeader().getKeyID());

            // Verify signature
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                log.error("Invalid JWT signature");
                return false;
            }

            // Check expiration
            Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                log.error("JWT token expired");
                return false;
            }

            // Validate FAPI claims
            return validateFAPIClaims(signedJWT);

        } catch (Exception e) {
            log.error("Error validating JWT token", e);
            return false;
        }
    }

    private boolean validateFAPIClaims(SignedJWT jwt) throws Exception {
        // Validate required FAPI claims
        String aud = jwt.getJWTClaimsSet().getAudience().getFirst();
        String azp = jwt.getJWTClaimsSet().getStringClaim("azp");
        String jti = jwt.getJWTClaimsSet().getJWTID();

        return aud != null && azp != null && jti != null;
    }
}
