// KafkaConsumerConfig.java (para consumir mensagens se necessário)
package br.com.openfinance.accounts.infrastructure.config;

import br.com.openfinance.accounts.application.event.AccountUpdateEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import reactor.kafka.receiver.ReceiverOptions;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:accounts-service}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:latest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.max-poll-records:500}")
    private Integer maxPollRecords;

    @Bean
    public ReactiveKafkaConsumerTemplate<String, AccountUpdateEvent> reactiveKafkaConsumerTemplate() {
        Map<String, Object> props = consumerProps();

        ReceiverOptions<String, AccountUpdateEvent> receiverOptions =
                ReceiverOptions.<String, AccountUpdateEvent>create(props)
                        .subscription(Collections.singleton("account-updates"))
                        .commitBatchSize(100)
                        .commitInterval(Duration.ofSeconds(5));

        return new ReactiveKafkaConsumerTemplate<>(receiverOptions);
    }

    private Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();

        // Configurações básicas
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Configurações de consumo
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 min

        // Configurações de performance
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        // Configurações do JsonDeserializer
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "br.com.openfinance.accounts.application.event");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AccountUpdateEvent.class);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return props;
    }
}
