package com.bank.openfinance.client.event.source.kafka;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.validation.annotation.Validated;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for both imperative and reactive implementations.
 * Supports Virtual Threads for imperative mode and Reactor Kafka for reactive mode.
 */
@Slf4j
@Data
@Configuration
@EnableKafka
@Validated
@ConfigurationProperties(prefix = "openfinance.kafka")
@ConditionalOnProperty(name = "openfinance.event.source", havingValue = "kafka")
public class KafkaConfiguration {

    // ===== Connection Properties =====
    @NotBlank
    private String bootstrapServers = "localhost:9092";

    private String schemaRegistryUrl;

    private SecurityConfig security = new SecurityConfig();

    // ===== Consumer Properties =====
    private ConsumerProperties consumer = new ConsumerProperties();

    // ===== Producer Properties =====
    private ProducerProperties producer = new ProducerProperties();

    // ===== Topic Configuration =====
    @NotBlank
    private String topic = "openfinance-events";

    private String dlqTopic = "openfinance-events-dlq";

    private TopicProperties topics = new TopicProperties();

    // ===== Performance Tuning =====
    private PerformanceConfig performance = new PerformanceConfig();

    // ===== Retry Configuration =====
    private RetryConfig retry = new RetryConfig();

    // ===== Inner Configuration Classes =====

    @Data
    public static class SecurityConfig {
        private boolean enabled = false;
        private String protocol = "SASL_SSL";
        private String saslMechanism = "PLAIN";
        private String saslJaasConfig;
        private String truststoreLocation;
        private String truststorePassword;
        private String keystoreLocation;
        private String keystorePassword;
        private String keyPassword;
    }

    @Data
    public static class ConsumerProperties {
        @NotBlank
        private String groupId = "openfinance-consumer";

        private String clientId = "openfinance-client";

        private boolean enableAutoCommit = false;

        private String autoOffsetReset = "earliest";

        @Positive
        private int maxPollRecords = 500;

        @Positive
        private int maxPollIntervalMs = 300000; // 5 minutes

        @Positive
        private int sessionTimeoutMs = 30000; // 30 seconds

        @Positive
        private int heartbeatIntervalMs = 3000; // 3 seconds

        @Positive
        private int fetchMinBytes = 1048576; // 1 MB

        @Positive
        private int fetchMaxWaitMs = 500;

        @Positive
        private int maxPartitionFetchBytes = 10485760; // 10 MB

        private int concurrency = 3;

        private String isolationLevel = "read_committed";
    }

    @Data
    public static class ProducerProperties {
        private String clientId = "openfinance-producer";

        private String acks = "1";

        @Positive
        private int retries = 3;

        @Positive
        private int batchSize = 16384;

        @Positive
        private int lingerMs = 10;

        @Positive
        private int bufferMemory = 33554432; // 32 MB

        private String compressionType = "lz4";

        @Positive
        private int maxRequestSize = 1048576; // 1 MB

        @Positive
        private int requestTimeoutMs = 30000;

        private String idempotenceEnabled = "true";

        private int maxInFlightRequestsPerConnection = 5;
    }

    @Data
    public static class TopicProperties {
        private Map<String, TopicConfig> configs = new HashMap<>();

        public TopicProperties() {
            // Default topic configurations
            configs.put("events", new TopicConfig("openfinance-events", 10, (short) 3));
            configs.put("dlq", new TopicConfig("openfinance-events-dlq", 3, (short) 3));
            configs.put("retry", new TopicConfig("openfinance-events-retry", 3, (short) 3));
        }
    }

    @Data
    public static class TopicConfig {
        private String name;
        private int partitions;
        private short replicationFactor;
        private Map<String, String> configs = new HashMap<>();

        public TopicConfig() {}

        public TopicConfig(String name, int partitions, short replicationFactor) {
            this.name = name;
            this.partitions = partitions;
            this.replicationFactor = replicationFactor;

            // Default topic-level configs
            this.configs.put("retention.ms", "604800000"); // 7 days
            this.configs.put("segment.ms", "86400000"); // 1 day
            this.configs.put("compression.type", "lz4");
        }
    }

    @Data
    public static class PerformanceConfig {
        private boolean virtualThreadsEnabled = true;

        @Positive
        private int virtualThreadPoolSize = 10000;

        @Positive
        private int reactorEventLoopThreads = 4;

        @Positive
        private int backpressureBufferSize = 10000;

        private boolean metricsEnabled = true;

        private boolean tracingEnabled = true;
    }

    @Data
    public static class RetryConfig {
        @Positive
        private int maxAttempts = 3;

        @Positive
        private long initialIntervalMs = 1000;

        @Positive
        private long maxIntervalMs = 10000;

        private double multiplier = 2.0;

        private boolean dlqEnabled = true;

        @Positive
        private int retryTopicAttempts = 3;
    }

    // ===== Bean Configurations =====

    /**
     * Kafka Consumer Factory for imperative/blocking consumption.
     * Optimized for Virtual Threads.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = consumerConfigs();

        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>();
        jsonDeserializer.addTrustedPackages("*");
        jsonDeserializer.setUseTypeHeaders(false);

        ErrorHandlingDeserializer<Object> errorHandlingDeserializer =
                new ErrorHandlingDeserializer<>(jsonDeserializer);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                errorHandlingDeserializer
        );
    }

    /**
     * Kafka Listener Container Factory with Virtual Thread support.
     */
    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Object>>
    kafkaListenerContainerFactory(ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(consumer.getConcurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setPollTimeout(3000);

        // Error handling with exponential backoff
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> {
                    log.error("Failed to process record after retries: {}", record, exception);
                    // Send to DLQ
                    sendToDlq(record, exception);
                },
                new ExponentialBackOff(retry.getInitialIntervalMs(), retry.getMultiplier())
        );
        errorHandler.setRetryListeners((record, ex, attempt) ->
                log.warn("Retry attempt {} for record: {}", attempt, record.key())
        );

        factory.setCommonErrorHandler(errorHandler);

        // Enable Virtual Threads if configured
        if (performance.isVirtualThreadsEnabled()) {
            factory.getContainerProperties().setListenerTaskExecutor(
                    java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()
            );
        }

        return factory;
    }

    /**
     * Kafka Producer Factory.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = producerConfigs();

        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>()
        );
    }

    /**
     * Kafka Template for sending messages.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {

        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        template.setDefaultTopic(topic);

        return template;
    }

    /**
     * Reactive Kafka Receiver Options.
     */
    @Bean
    @ConditionalOnProperty(name = "openfinance.client.type", havingValue = "reactive")
    public ReceiverOptions<String, Object> kafkaReceiverOptions() {
        Map<String, Object> props = consumerConfigs();

        return ReceiverOptions.<String, Object>create(props)
                .consumerProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumer.getAutoOffsetReset())
                .subscription(java.util.Collections.singletonList(topic))
                .addAssignListener(partitions ->
                        log.info("Partitions assigned: {}", partitions))
                .addRevokeListener(partitions ->
                        log.info("Partitions revoked: {}", partitions));
    }

    /**
     * Reactive Kafka Sender.
     */
    @Bean
    @ConditionalOnProperty(name = "openfinance.client.type", havingValue = "reactive")
    public KafkaSender<String, Object> kafkaSender() {
        Map<String, Object> props = producerConfigs();

        SenderOptions<String, Object> senderOptions = SenderOptions.<String, Object>create(props)
                .maxInFlight(producer.getMaxInFlightRequestsPerConnection())
                .stopOnError(false);

        return KafkaSender.create(senderOptions);
    }

    /**
     * Admin client for topic management.
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        if (security.isEnabled()) {
            addSecurityConfigs(configs);
        }

        return new KafkaAdmin(configs);
    }

    // ===== Helper Methods =====

    private Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<>();

        // Connection
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Consumer Group
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumer.getGroupId());
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, consumer.getClientId());

        // Offset Management
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, consumer.isEnableAutoCommit());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumer.getAutoOffsetReset());

        // Performance
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, consumer.getMaxPollRecords());
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, consumer.getMaxPollIntervalMs());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, consumer.getSessionTimeoutMs());
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, consumer.getHeartbeatIntervalMs());
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, consumer.getFetchMinBytes());
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, consumer.getFetchMaxWaitMs());
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, consumer.getMaxPartitionFetchBytes());

        // Isolation Level
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, consumer.getIsolationLevel());

        // Serialization
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Map.class);

        // Security
        if (security.isEnabled()) {
            addSecurityConfigs(props);
        }

        // Metrics
        if (performance.isMetricsEnabled()) {
            props.put(ConsumerConfig.METRICS_RECORDING_LEVEL_CONFIG, "INFO");
            props.put(ConsumerConfig.METRIC_REPORTER_CLASSES_CONFIG,
                    "io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics");
        }

        return props;
    }

    private Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();

        // Connection
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Client ID
        props.put(ProducerConfig.CLIENT_ID_CONFIG, producer.getClientId());

        // Reliability
        props.put(ProducerConfig.ACKS_CONFIG, producer.getAcks());
        props.put(ProducerConfig.RETRIES_CONFIG, producer.getRetries());
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, producer.getIdempotenceEnabled());
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
                producer.getMaxInFlightRequestsPerConnection());

        // Performance
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, producer.getBatchSize());
        props.put(ProducerConfig.LINGER_MS_CONFIG, producer.getLingerMs());
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, producer.getBufferMemory());
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, producer.getCompressionType());

        // Request Limits
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, producer.getMaxRequestSize());
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, producer.getRequestTimeoutMs());

        // Serialization
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Security
        if (security.isEnabled()) {
            addSecurityConfigs(props);
        }

        // Metrics
        if (performance.isMetricsEnabled()) {
            props.put(ProducerConfig.METRICS_RECORDING_LEVEL_CONFIG, "INFO");
        }

        return props;
    }

    private void addSecurityConfigs(Map<String, Object> props) {
        props.put("security.protocol", security.getProtocol());

        if ("SASL_SSL".equals(security.getProtocol()) || "SASL_PLAINTEXT".equals(security.getProtocol())) {
            props.put("sasl.mechanism", security.getSaslMechanism());
            props.put("sasl.jaas.config", security.getSaslJaasConfig());
        }

        if ("SSL".equals(security.getProtocol()) || "SASL_SSL".equals(security.getProtocol())) {
            if (security.getTruststoreLocation() != null) {
                props.put("ssl.truststore.location", security.getTruststoreLocation());
                props.put("ssl.truststore.password", security.getTruststorePassword());
            }

            if (security.getKeystoreLocation() != null) {
                props.put("ssl.keystore.location", security.getKeystoreLocation());
                props.put("ssl.keystore.password", security.getKeystorePassword());
                props.put("ssl.key.password", security.getKeyPassword());
            }
        }
    }

    private void sendToDlq(Object record, Exception exception) {
        // Implementation for sending failed records to DLQ
        log.error("Sending record to DLQ: {}", record, exception);
        // Use kafkaTemplate to send to DLQ topic
    }

    // ===== Getters for commonly used configurations =====

    public String getGroupId() {
        return consumer.getGroupId();
    }

    public int getMaxPollRecords() {
        return consumer.getMaxPollRecords();
    }

    public int getFetchMinBytes() {
        return consumer.getFetchMinBytes();
    }

    public int getFetchMaxWaitMs() {
        return consumer.getFetchMaxWaitMs();
    }

    public Duration getPollTimeout() {
        return Duration.ofMillis(3000);
    }

    public boolean isVirtualThreadsEnabled() {
        return performance.isVirtualThreadsEnabled();
    }
}
