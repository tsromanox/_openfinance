// KafkaConfig.java
package br.com.openfinance.accounts.infrastructure.config;

import br.com.openfinance.accounts.application.event.AccountUpdateEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.retries:3}")
    private Integer retries;

    @Value("${spring.kafka.producer.batch-size:16384}")
    private Integer batchSize;

    @Value("${spring.kafka.producer.linger-ms:10}")
    private Integer lingerMs;

    @Value("${spring.kafka.producer.compression-type:snappy}")
    private String compressionType;

    @Value("${spring.kafka.producer.max-in-flight-requests-per-connection:5}")
    private Integer maxInFlightRequests;

    @Bean
    public ReactiveKafkaProducerTemplate<String, AccountUpdateEvent> reactiveKafkaProducerTemplate() {
        Map<String, Object> props = producerProps();

        SenderOptions<String, AccountUpdateEvent> senderOptions =
                SenderOptions.<String, AccountUpdateEvent>create(props)
                        .maxInFlight(1024);

        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }

    @Bean
    public KafkaSender<String, AccountUpdateEvent> kafkaSender() {
        Map<String, Object> props = producerProps();

        SenderOptions<String, AccountUpdateEvent> senderOptions =
                SenderOptions.<String, AccountUpdateEvent>create(props)
                        .maxInFlight(1024)
                        .stopOnError(false);

        return KafkaSender.create(senderOptions);
    }

    private Map<String, Object> producerProps() {
        Map<String, Object> props = new HashMap<>();

        // Configurações básicas
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Configurações de confiabilidade
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        props.put(ProducerConfig.RETRIES_CONFIG, retries);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequests);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Configurações de performance
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB

        // Configurações de timeout
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        // Configurações do JsonSerializer
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        //props.put(JsonSerializer.USE_TYPE_INFO_HEADERS, false);

        return props;
    }
}
