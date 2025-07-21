package br.com.openfinance.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
public class OpenFinanceClientFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String interactionId = UUID.randomUUID().toString();

        ClientRequest newRequest = ClientRequest.from(request)
                .header("x-fapi-interaction-id", interactionId)
                .header("x-fapi-auth-date", generateAuthDate())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();

        long startTime = System.currentTimeMillis();

        return next.exchange(newRequest)
                .doOnSuccess(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Request to {} completed in {}ms with status {}",
                            request.url(), duration, response.statusCode());
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Request to {} failed after {}ms: {}",
                            request.url(), duration, error.getMessage());
                });
    }

    private String generateAuthDate() {
        // Format: Sun, 10 Sep 2017 19:43:31 UTC
        return java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
