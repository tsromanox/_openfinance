package com.bank.openfinance.client.event.source.kafka;

import com.bank.openfinance.client.event.model.EventEnvelope;
import com.bank.openfinance.client.event.source.EventSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "openfinance.event.source", havingValue = "kafka")
public class KafkaEventSource implements EventSource {

    private final KafkaConfiguration kafkaConfig;
    private KafkaConsumer<String, Map<String, Object>> consumer;
    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaConfig.getMaxPollRecords());
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, kafkaConfig.getFetchMinBytes());
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, kafkaConfig.getFetchMaxWaitMs());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Map.class);

        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(kafkaConfig.getTopic()));

        log.info("Kafka consumer initialized for topic: {}", kafkaConfig.getTopic());
    }

    @Override
    public Stream<EventEnvelope<?>> consumeBlocking() {
        return Stream.generate(() -> {
            if (!running) {
                return Collections.<EventEnvelope<?>>emptyList();
            }

            ConsumerRecords<String, Map<String, Object>> records =
                    consumer.poll(Duration.ofMillis(100));

            if (!records.isEmpty()) {
                List<EventEnvelope<?>> events = new ArrayList<>();

                // Process with virtual threads for parallel processing
                try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
                    var tasks = StreamSupport.stream(records.spliterator(), false)
                            .map(record -> scope.fork(() -> createEnvelope(record)))
                            .toList();

                    scope.join();
                    scope.throwIfFailed();

                    tasks.forEach(task -> events.add(task.get()));

                    // Commit after successful processing
                    consumer.commitSync();

                    log.debug("Processed {} events from Kafka", events.size());

                } catch (Exception e) {
                    log.error("Error processing Kafka batch", e);
                    // Don't commit on error - messages will be reprocessed
                }

                return events;
            }

            return Collections.<EventEnvelope<?>>emptyList();
        }).flatMap(List::stream);
    }

    @Override
    public Flux<EventEnvelope<?>> consumeReactive() {
        ReceiverOptions<String, Map<String, Object>> receiverOptions = createReceiverOptions();

        return KafkaReceiver.create(receiverOptions)
                .receive()
                .map(this::createEnvelopeFromReactive)
                .doOnNext(envelope -> log.debug("Received event: {}", envelope.getEventType()))
                .doOnError(error -> log.error("Error in Kafka reactive consumer", error))
                .retry(3);
    }

    private EventEnvelope<?> createEnvelope(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> headers = new HashMap<>();
        record.headers().forEach(header ->
                headers.put(header.key(), new String(header.value()))
        );

        return EventEnvelope.builder()
                .eventId(UUID.fromString(
                        headers.getOrDefault("eventId", UUID.randomUUID().toString()).toString()))
                .eventType(headers.getOrDefault("eventType", "UNKNOWN").toString())
                .source("kafka")
                .timestamp(Instant.ofEpochMilli(record.timestamp()))
                .correlationId(headers.getOrDefault("correlationId", "").toString())
                .payload(record.value())
                .headers(headers)
                .build();
    }

    private EventEnvelope<?> createEnvelopeFromReactive(ReceiverRecord<String, Map<String, Object>> record) {
        return createEnvelope(record);
    }

    private ReceiverOptions<String, Map<String, Object>> createReceiverOptions() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return ReceiverOptions.<String, Map<String, Object>>create(props)
                .subscription(Collections.singleton(kafkaConfig.getTopic()))
                .addAssignListener(partitions ->
                        log.info("Partitions assigned: {}", partitions))
                .addRevokeListener(partitions ->
                        log.info("Partitions revoked: {}", partitions));
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        if (consumer != null) {
            consumer.close();
            log.info("Kafka consumer closed");
        }
    }
}