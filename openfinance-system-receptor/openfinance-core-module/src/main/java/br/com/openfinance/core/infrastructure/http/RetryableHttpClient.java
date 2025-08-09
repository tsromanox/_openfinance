package br.com.openfinance.core.infrastructure.http;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RetryableHttpClient {

    private final RestTemplate restTemplate;

    public RetryableHttpClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public <T> ResponseEntity<T> get(String url,
                                     HttpHeaders headers,
                                     Class<T> responseType) {
        log.info("Making GET request to: {}", url);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
    }
}
