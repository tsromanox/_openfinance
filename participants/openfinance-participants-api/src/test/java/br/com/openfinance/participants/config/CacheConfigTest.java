package br.com.openfinance.participants.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void shouldHaveParticipantsCache() {
        assertThat(cacheManager.getCache("participants")).isNotNull();
    }

    @Test
    void shouldHaveApiEndpointsCache() {
        assertThat(cacheManager.getCache("apiEndpoints")).isNotNull();
    }
}
