package br.com.openfinance.consents.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    public <T> Mono<T> checkIdempotency(String key, Class<T> type) {
        String redisKey = IDEMPOTENCY_PREFIX + key;

        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(json -> Mono.fromCallable(() -> objectMapper.readValue(json, type)))
                .doOnNext(result -> log.debug("Idempotency hit for key {}", key))
                .onErrorResume(error -> {
                    log.error("Error checking idempotency", error);
                    return Mono.empty();
                });
    }

    public <T> Mono<T> saveIdempotency(String key, T value, Class<T> type) {
        String redisKey = IDEMPOTENCY_PREFIX + key;

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(value))
                .flatMap(json -> redisTemplate.opsForValue().set(redisKey, json, IDEMPOTENCY_TTL))
                .thenReturn(value)
                .doOnSuccess(v -> log.debug("Saved idempotency for key {}", key))
                .onErrorResume(error -> {
                    log.error("Error saving idempotency", error);
                    return Mono.just(value);
                });
    }
}
