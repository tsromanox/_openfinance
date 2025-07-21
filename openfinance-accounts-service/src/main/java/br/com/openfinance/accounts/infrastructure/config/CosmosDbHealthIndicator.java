package br.com.openfinance.accounts.infrastructure.config;

import com.azure.cosmos.CosmosAsyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class CosmosDbHealthIndicator implements ReactiveHealthIndicator {

    private final CosmosAsyncClient cosmosAsyncClient;

    @Value("${azure.cosmos.database}")
    private String databaseName;

    @Override
    public Mono<Health> health() {
        return cosmosAsyncClient.getDatabase(databaseName)
                .read()
                .map(response -> {
                    if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                        return Health.up()
                                .withDetail("database", databaseName)
                                .withDetail("statusCode", response.getStatusCode())
                                .withDetail("requestCharge", response.getRequestCharge())
                                .build();
                    } else {
                        return Health.down()
                                .withDetail("database", databaseName)
                                .withDetail("statusCode", response.getStatusCode())
                                .build();
                    }
                })
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(error -> {
                    log.error("Cosmos DB health check failed", error);
                    return Mono.just(Health.down()
                            .withDetail("database", databaseName)
                            .withException(error)
                            .build());
                });
    }
}