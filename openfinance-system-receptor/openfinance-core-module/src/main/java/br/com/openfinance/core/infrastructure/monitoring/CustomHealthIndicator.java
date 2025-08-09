package br.com.openfinance.core.infrastructure.monitoring;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Health health() {
        try {
            // Check database
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            // Check Kafka
            kafkaTemplate.send("health-check", "test").get();

            return Health.up()
                    .withDetail("database", "UP")
                    .withDetail("kafka", "UP")
                    .build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
