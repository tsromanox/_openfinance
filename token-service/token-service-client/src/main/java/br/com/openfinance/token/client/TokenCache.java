package br.com.openfinance.token.client;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenCache {

    private final Map<String, CachedToken> cache;
    private final int maxSize;
    private final Duration defaultTtl;

    public TokenCache(int maxSize, Duration defaultTtl) {
        this.cache = new ConcurrentHashMap<>();
        this.maxSize = maxSize;
        this.defaultTtl = defaultTtl;
    }

    public void put(String key, String token, Duration ttl) {
        if (cache.size() >= maxSize) {
            evictOldest();
        }

        var expiresAt = Instant.now().plus(ttl != null ? ttl : defaultTtl);
        cache.put(key, new CachedToken(token, expiresAt));
    }

    public String get(String key) {
        var cached = cache.get(key);
        if (cached != null) {
            if (cached.isValid()) {
                return cached.token;
            } else {
                cache.remove(key);
            }
        }
        return null;
    }

    public void evictByToken(String token) {
        cache.entrySet().removeIf(entry ->
                entry.getValue().token.equals(token)
        );
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    private void evictOldest() {
        cache.entrySet().stream()
                .min((e1, e2) -> e1.getValue().expiresAt.compareTo(e2.getValue().expiresAt))
                .ifPresent(entry -> cache.remove(entry.getKey()));
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
