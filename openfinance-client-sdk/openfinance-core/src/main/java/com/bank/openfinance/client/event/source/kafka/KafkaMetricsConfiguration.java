package com.bank.openfinance.client.event.source.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Configuration for Kafka metrics.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "openfinance.kafka.performance.metrics-enabled", havingValue = "true", matchIfMissing = true)
public class KafkaMetricsConfiguration {

    @Bean
    public KafkaClientMetrics kafkaConsumerMetrics(
            ConsumerFactory<String, Object> consumerFactory,
            MeterRegistry meterRegistry) {

        Consumer<String, Object> consumer = consumerFactory.createConsumer();
        KafkaClientMetrics metrics = new KafkaClientMetrics(consumer);
        metrics.bindTo(meterRegistry);

        log.info("Kafka consumer metrics configured");
        return metrics;
    }

    @Bean
    public KafkaClientMetrics kafkaProducerMetrics(
            ProducerFactory<String, Object> producerFactory,
            MeterRegistry meterRegistry) {

        Producer<String, Object> producer = producerFactory.createProducer();
        KafkaClientMetrics metrics = new KafkaClientMetrics(producer);
        metrics.bindTo(meterRegistry);

        log.info("Kafka producer metrics configured");
        return metrics;
    }
}
