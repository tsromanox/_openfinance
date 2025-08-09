package br.com.openfinance.consents.infrastructure.adapter.cache;

import br.com.openfinance.consents.domain.entity.Consent;
import br.com.openfinance.consents.domain.port.out.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentRedisCache implements CacheService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CONSENT_KEY_PREFIX = "consent:";
    private static final String CLIENT_CONSENTS_KEY_PREFIX = "client-consents:";

    @Override
    public Mono<Void> putConsent(String key, Consent consent, Duration ttl) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(consent))
                .flatMap(json -> redisTemplate.opsForValue().set(key, json, ttl))
                .doOnSuccess(v -> log.debug("Cached consent with key {}", key))
                .doOnError(error -> log.error("Error caching consent", error))
                .then();
    }

    @Override
    public Mono<Consent> getConsent(String key) {
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> Mono.fromCallable(() -> objectMapper.readValue(json, Consent.class)))
                .doOnNext(consent -> log.debug("Retrieved consent from cache with key {}", key))
                .doOnError(error -> log.error("Error retrieving consent from cache", error))
                .onErrorResume(throwable -> Mono.empty());
    }

    @Override
    public Mono<Boolean> evictConsent(String key) {
        return redisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnSuccess(deleted -> {
                    if (deleted) {
                        log.debug("Evicted consent with key {}", key);
                    }
                });
    }

    @Override
    public Mono<Boolean> evictAllConsentsForClient(String clientId) {
        String pattern = CLIENT_CONSENTS_KEY_PREFIX + clientId + ":*";

        return redisTemplate.keys(pattern)
                .flatMap(key -> redisTemplate.delete(key))
                .collectList()
                .map(results -> !results.isEmpty())
                .doOnSuccess(deleted -> {
                    if (deleted) {
                        log.debug("Evicted all consents for client {}", clientId);
                    }
                });
    }
}
