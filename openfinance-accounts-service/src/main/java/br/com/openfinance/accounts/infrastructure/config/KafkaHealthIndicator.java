// KafkaHealthIndicator.java
package br.com.openfinance.accounts.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaHealthIndicator implements ReactiveHealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    @Override
    public Mono<Health> health() {
        return Mono.fromCallable(() -> {
                    try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                        DescribeClusterResult clusterResult = adminClient.describeCluster();

                        int nodeCount = clusterResult.nodes().get(5, TimeUnit.SECONDS).size();
                        String clusterId = clusterResult.clusterId().get(5, TimeUnit.SECONDS);

                        return Health.up()
                                .withDetail("clusterId", clusterId)
                                .withDetail("nodeCount", nodeCount)
                                .build();

                    } catch (Exception e) {
                        log.error("Kafka health check failed", e);
                        return Health.down()
                                .withException(e)
                                .build();
                    }
                })
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(error -> Mono.just(Health.down().withException(error).build()));
    }
}
