// KafkaTopicConfig.java (criação automática de tópicos)
package br.com.openfinance.accounts.infrastructure.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic accountUpdatesTopic() {
        return TopicBuilder.name("account-updates")
                .partitions(10)
                .replicas(3)
                .config("retention.ms", "86400000") // 1 dia
                .config("compression.type", "snappy")
                .config("segment.ms", "3600000") // 1 hora
                .build();
    }

    @Bean
    public NewTopic consentUpdatesTopic() {
        return TopicBuilder.name("consent-updates")
                .partitions(10)
                .replicas(3)
                .config("retention.ms", "86400000") // 1 dia
                .config("compression.type", "snappy")
                .build();
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name("account-updates-dlq")
                .partitions(5)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 dias
                .build();
    }
}
