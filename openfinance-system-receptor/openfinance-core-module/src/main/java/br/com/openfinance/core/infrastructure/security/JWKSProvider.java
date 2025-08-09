package br.com.openfinance.core.infrastructure.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class JWKSProvider {

    @Value("${security.oauth2.resource.jwk.key-set-uri}")
    private String jwkSetUri;

    private final RestTemplate restTemplate;
    private final Map<String, RSAPublicKey> keyCache = new ConcurrentHashMap<>();

    public JWKSProvider() {
        this.restTemplate = new RestTemplate();
    }

    @Cacheable(value = "jwks", key = "#keyId")
    public RSAPublicKey getPublicKey(String keyId) {
        return keyCache.computeIfAbsent(keyId, k -> {
            try {
                JWKSet jwkSet = JWKSet.load(new URL(jwkSetUri));
                JWK jwk = jwkSet.getKeyByKeyId(keyId);

                if (jwk instanceof RSAKey) {
                    RSAKey rsaKey = (RSAKey) jwk;
                    return rsaKey.toRSAPublicKey();
                }

                throw new IllegalArgumentException("Key not found or not RSA: " + keyId);
            } catch (Exception e) {
                log.error("Error loading JWK for keyId: {}", keyId, e);
                throw new RuntimeException("Failed to load public key", e);
            }
        });
    }

    public void refreshKeys() {
        keyCache.clear();
        log.info("JWK cache cleared");
    }
}
