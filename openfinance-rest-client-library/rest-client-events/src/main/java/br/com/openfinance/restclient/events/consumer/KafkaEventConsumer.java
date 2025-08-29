package br.com.openfinance.restclient.events.consumer;

import br.com.openfinance.restclient.core.event.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Event consumer that consumes events from Kafka topics.
 * Supports both reactive (reactor-kafka) and imperative (spring-kafka) consumption patterns.
 * 
 * Features:
 * - Configurable consumer groups and topics
 * - Automatic offset management
 * - Error handling and retry logic
 * - Metrics collection
 * - Backpressure handling for reactive streams
 */
@Slf4j
public class KafkaEventConsumer<T> implements EventConsumer<T> {

    private final EventConsumerConfiguration configuration;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Class<T> payloadType;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // Reactive Kafka components
    private KafkaReceiver<String, String> kafkaReceiver;
    
    // Imperative Kafka components
    private KafkaMessageListenerContainer<String, String> listenerContainer;
    private Sinks.Many<EventEnvelope<T>> eventSink;

    public KafkaEventConsumer(EventConsumerConfiguration configuration,
                            ObjectMapper objectMapper,
                            MeterRegistry meterRegistry,
                            Class<T> payloadType) {
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.payloadType = payloadType;
        
        initializeKafkaComponents();
    }

    /**
     * Initialize Kafka receiver and listener components
     */
    private void initializeKafkaComponents() {
        var kafkaConfig = configuration.getKafka();
        
        if (kafkaConfig.isReactive()) {
            initializeReactiveKafka();
        } else {
            initializeImperativeKafka();
        }
    }

    /**
     * Initialize reactive Kafka receiver
     */
    private void initializeReactiveKafka() {
        var kafkaConfig = configuration.getKafka();
        Map<String, Object> props = createKafkaProperties();
        
        ReceiverOptions<String, String> receiverOptions = ReceiverOptions
                .<String, String>create(props)
                .subscription(Collections.singleton(kafkaConfig.getTopic()));
                
        this.kafkaReceiver = KafkaReceiver.create(receiverOptions);
        
        log.info("Initialized reactive Kafka receiver for topic: {} with group: {}", 
                kafkaConfig.getTopic(), kafkaConfig.getGroupId());
    }

    /**
     * Initialize imperative Kafka listener
     */
    private void initializeImperativeKafka() {
        var kafkaConfig = configuration.getKafka();
        Map<String, Object> props = createKafkaProperties();
        
        ConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(props);
        
        ContainerProperties containerProperties = new ContainerProperties(kafkaConfig.getTopic());
        containerProperties.setMessageListener(new ImperativeMessageListener());
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        this.listenerContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        this.eventSink = Sinks.many().multicast().onBackpressureBuffer(configuration.getBufferSize());
        
        log.info("Initialized imperative Kafka listener for topic: {} with group: {}", 
                kafkaConfig.getTopic(), kafkaConfig.getGroupId());
    }

    /**
     * Create Kafka consumer properties
     */
    private Map<String, Object> createKafkaProperties() {
        var kafkaConfig = configuration.getKafka();
        Map<String, Object> props = new HashMap<>();
        
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfig.getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConfig.getAutoOffsetReset());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaConfig.isEnableAutoCommit());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, (int) kafkaConfig.getSessionTimeout().toMillis());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, configuration.getBatchSize());
        
        // Add additional properties if configured
        if (kafkaConfig.getAdditionalProperties() != null) {
            props.putAll(kafkaConfig.getAdditionalProperties());
        }
        
        return props;
    }

    @Override
    public Flux<EventEnvelope<T>> consume() {
        if (!running.compareAndSet(false, true)) {
            return Flux.error(new IllegalStateException("Consumer is already running"));
        }

        log.info("Starting Kafka event consumer for topic: {}", 
                configuration.getKafka().getTopic());

        if (configuration.getKafka().isReactive()) {
            return consumeReactive();
        } else {
            return consumeImperative();
        }
    }

    /**
     * Reactive event consumption using reactor-kafka
     */
    private Flux<EventEnvelope<T>> consumeReactive() {
        return kafkaReceiver.receive()
                .onBackpressureBuffer(configuration.getBufferSize())
                .map(this::convertReceiverRecord)
                .filter(envelope -> envelope != null) // Filter out failed conversions
                .doOnNext(this::recordSuccessMetric)
                .doOnError(this::recordErrorMetric)
                .onErrorContinue((throwable, obj) -> 
                    log.error("Error processing Kafka message: {}", obj, throwable))
                .doOnSubscribe(sub -> meterRegistry.gauge("kafka_event_consumer_running", 1.0))
                .doFinally(sig -> {
                    running.set(false);
                    meterRegistry.gauge("kafka_event_consumer_running", 0.0);
                })
                .share();
    }

    /**
     * Imperative event consumption using spring-kafka
     */
    private Flux<EventEnvelope<T>> consumeImperative() {
        listenerContainer.start();
        
        return eventSink.asFlux()
                .onBackpressureBuffer(configuration.getBufferSize())
                .doOnNext(this::recordSuccessMetric)
                .doOnSubscribe(sub -> meterRegistry.gauge("kafka_event_consumer_running", 1.0))
                .doFinally(sig -> {
                    listenerContainer.stop();
                    running.set(false);
                    meterRegistry.gauge("kafka_event_consumer_running", 0.0);
                })
                .share();
    }

    /**
     * Convert reactor-kafka ReceiverRecord to EventEnvelope
     */
    private EventEnvelope<T> convertReceiverRecord(ReceiverRecord<String, String> record) {
        try {
            ConsumerRecord<String, String> consumerRecord = record.record();
            EventEnvelope<T> envelope = convertKafkaRecord(consumerRecord);
            
            // Acknowledge the message
            record.receiverOffset().acknowledge();
            
            return envelope;
            
        } catch (Exception e) {
            log.error("Failed to convert Kafka record to EventEnvelope", e);
            recordErrorMetric(e);
            return null; // Will be filtered out
        }
    }

    /**
     * Convert Kafka ConsumerRecord to EventEnvelope
     */
    private EventEnvelope<T> convertKafkaRecord(ConsumerRecord<String, String> record) throws JsonProcessingException {
        // Parse the event envelope from JSON
        EventEnvelope<String> rawEnvelope = objectMapper.readValue(record.value(), EventEnvelope.class);
        
        // Convert payload to the target type
        T payload = objectMapper.convertValue(rawEnvelope.getPayload(), payloadType);
        
        return EventEnvelope.<T>builder()
                .eventId(rawEnvelope.getEventId())
                .eventType(rawEnvelope.getEventType())
                .source(EventEnvelope.EventSource.KAFKA)
                .timestamp(rawEnvelope.getTimestamp() != null ? 
                          rawEnvelope.getTimestamp() : 
                          OffsetDateTime.ofInstant(
                              java.time.Instant.ofEpochMilli(record.timestamp()), 
                              java.time.ZoneOffset.UTC))
                .payload(payload)
                .correlationId(rawEnvelope.getCorrelationId())
                .version(rawEnvelope.getVersion())
                .retryAttempt(rawEnvelope.getRetryAttempt())
                .maxRetryAttempts(configuration.getMaxRetryAttempts())
                .headers(rawEnvelope.getHeaders())
                .priority(rawEnvelope.getPriority())
                .targetEndpoint(rawEnvelope.getTargetEndpoint())
                .httpMethod(rawEnvelope.getHttpMethod())
                .partitionKey(record.key())
                .build();
    }

    /**
     * Imperative message listener for spring-kafka
     */
    private class ImperativeMessageListener implements MessageListener<String, String> {
        @Override
        public void onMessage(ConsumerRecord<String, String> record) {
            try {
                EventEnvelope<T> envelope = convertKafkaRecord(record);
                eventSink.tryEmitNext(envelope);
                
            } catch (Exception e) {
                log.error("Failed to process Kafka message", e);
                recordErrorMetric(e);
                // Consider implementing dead letter queue here
            }
        }
    }

    /**
     * Record success metrics
     */
    private void recordSuccessMetric(EventEnvelope<T> envelope) {
        meterRegistry.counter("kafka_events_consumed_total",
                "topic", configuration.getKafka().getTopic(),
                "group", configuration.getKafka().getGroupId(),
                "status", "success").increment();
    }

    /**
     * Record error metrics
     */
    private void recordErrorMetric(Throwable throwable) {
        meterRegistry.counter("kafka_events_consumed_total",
                "topic", configuration.getKafka().getTopic(),
                "group", configuration.getKafka().getGroupId(),
                "status", "error").increment();
    }

    @Override
    public void stop() {
        log.info("Stopping Kafka event consumer");
        running.set(false);
        
        if (listenerContainer != null) {
            listenerContainer.stop();
        }
        
        if (eventSink != null) {
            eventSink.tryEmitComplete();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public EventConsumerConfiguration getConfiguration() {
        return configuration;
    }
}