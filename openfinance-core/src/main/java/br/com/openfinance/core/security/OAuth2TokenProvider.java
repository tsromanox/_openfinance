package br.com.openfinance.core.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2TokenProvider {

    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    @Cacheable(value = "oauth2-tokens", unless = "#result == null")
    public Mono<String> getToken() {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("openfinance")
                .principal("openfinance-client")
                .build();

        return authorizedClientManager.authorize(authorizeRequest)
                .map(client -> client.getAccessToken())
                .map(OAuth2AccessToken::getTokenValue)
                .doOnNext(token -> log.debug("OAuth2 token obtained successfully"))
                .doOnError(error -> log.error("Failed to obtain OAuth2 token", error));
    }

    public void evictTokenCache() {
        // Implementado via @CacheEvict em outro método
    }
}
