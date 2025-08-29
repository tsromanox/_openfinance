package br.com.openfinance.restclient.starter;

import br.com.openfinance.restclient.core.client.ApiClient;
import br.com.openfinance.restclient.core.client.RestClientStrategy;
import br.com.openfinance.restclient.core.config.ClientConfiguration;
import br.com.openfinance.restclient.events.consumer.EventConsumer;
import br.com.openfinance.restclient.events.consumer.EventConsumerConfiguration;
import br.com.openfinance.restclient.imperative.ImperativeApiClient;
import br.com.openfinance.restclient.reactive.ReactiveApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Auto-configuration for the OpenFinance REST Client Library.
 * 
 * This configuration class automatically sets up the appropriate client implementation
 * (reactive or imperative) based on the application's dependencies and configuration.
 * 
 * Features:
 * - Automatic detection of web stack (WebFlux vs WebMVC)
 * - Configurable client strategy selection
 * - High-performance connection pooling
 * - Resilience patterns integration
 * - Event consumer configuration
 * - Virtual Threads setup for Java 21
 */
@Slf4j
@AutoConfiguration(after = {
    WebFluxAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    R2dbcDataAutoConfiguration.class,
    JdbcRepositoriesAutoConfiguration.class,
    KafkaAutoConfiguration.class,
    MetricsAutoConfiguration.class
})
@EnableConfigurationProperties({
    RestClientProperties.class,
    EventConsumerProperties.class
})
public class RestClientAutoConfiguration {

    /**
     * Configuration for reactive clients (WebFlux stack)
     */
    @Configuration
    @ConditionalOnClass({WebClient.class, reactor.core.publisher.Mono.class})
    @ConditionalOnProperty(name = "openfinance.rest-client.strategy", havingValue = "reactive", matchIfMissing = true)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    static class ReactiveClientConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public WebClient.Builder webClientBuilder(RestClientProperties properties) {
            var config = properties.toClientConfiguration();
            
            // Configure connection provider for high concurrency
            ConnectionProvider connectionProvider = ConnectionProvider.builder("rest-client-pool")
                    .maxConnections(config.getMaxConnections())
                    .maxIdleTime(config.getIdleTimeout())
                    .maxLifeTime(config.getKeepAliveTimeout())
                    .pendingAcquireMaxCount(config.getMaxConnections() * 2)
                    .build();

            HttpClient httpClient = HttpClient.create(connectionProvider)
                    .responseTimeout(config.getReadTimeout())
                    .followRedirect(true);

            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .defaultHeaders(headers -> {
                        if (config.getDefaultHeaders() != null) {
                            config.getDefaultHeaders().forEach(headers::add);
                        }
                    });
        }

        @Bean
        @ConditionalOnMissingBean
        public WebClient webClient(WebClient.Builder builder, RestClientProperties properties) {
            return builder.baseUrl(properties.getBaseUrl()).build();
        }

        @Bean
        @ConditionalOnMissingBean
        public ReactiveApiClient reactiveApiClient(
                WebClient webClient,
                RestClientProperties properties,
                CircuitBreaker circuitBreaker,
                Retry retry,
                RateLimiter rateLimiter,
                MeterRegistry meterRegistry) {

            log.info("Creating ReactiveApiClient with base URL: {}", properties.getBaseUrl());
            return new ReactiveApiClient(
                    webClient,
                    properties.toClientConfiguration(),
                    circuitBreaker,
                    retry,
                    rateLimiter,
                    meterRegistry);
        }
    }

    /**
     * Configuration for imperative clients (WebMVC stack with Virtual Threads)
     */
    @Configuration
    @ConditionalOnClass({RestClient.class})
    @ConditionalOnProperty(name = "openfinance.rest-client.strategy", havingValue = "imperative")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class ImperativeClientConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RestClient.Builder restClientBuilder(RestClientProperties properties) {
            var config = properties.toClientConfiguration();
            
            return RestClient.builder()
                    .baseUrl(properties.getBaseUrl())
                    .defaultHeaders(headers -> {
                        if (config.getDefaultHeaders() != null) {
                            config.getDefaultHeaders().forEach(headers::add);
                        }
                    })
                    .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(
                            java.net.http.HttpClient.newBuilder()
                                    .connectTimeout(config.getConnectionTimeout())
                                    .executor(Executors.newVirtualThreadPerTaskExecutor())
                                    .build()));
        }

        @Bean
        @ConditionalOnMissingBean
        public RestClient restClient(RestClient.Builder builder) {
            return builder.build();
        }

        @Bean
        @ConditionalOnMissingBean
        public ImperativeApiClient imperativeApiClient(
                RestClient restClient,
                RestClientProperties properties,
                CircuitBreaker circuitBreaker,
                Retry retry,
                RateLimiter rateLimiter,
                MeterRegistry meterRegistry) {

            log.info("Creating ImperativeApiClient with Virtual Threads, base URL: {}", properties.getBaseUrl());
            return new ImperativeApiClient(
                    restClient,
                    properties.toClientConfiguration(),
                    circuitBreaker,
                    retry,
                    rateLimiter,
                    meterRegistry);
        }
    }

    /**
     * Common resilience configuration
     */
    @Configuration
    static class ResilienceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public CircuitBreakerRegistry circuitBreakerRegistry(RestClientProperties properties) {
            var resilience = properties.getResilience();
            
            return CircuitBreakerRegistry.of(
                    io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                            .failureRateThreshold(resilience.getFailureRateThreshold())
                            .minimumNumberOfCalls(resilience.getMinimumNumberOfCalls())
                            .slidingWindowSize(resilience.getSlidingWindowSize())
                            .waitDurationInOpenState(resilience.getWaitDurationInOpenState())
                            .build());
        }

        @Bean
        @ConditionalOnMissingBean
        public CircuitBreaker circuitBreaker(CircuitBreakerRegistry registry) {
            return registry.circuitBreaker("rest-client");
        }

        @Bean
        @ConditionalOnMissingBean
        public RetryRegistry retryRegistry(RestClientProperties properties) {
            var resilience = properties.getResilience();
            
            return RetryRegistry.of(
                    io.github.resilience4j.retry.RetryConfig.custom()
                            .maxAttempts(resilience.getMaxRetryAttempts())
                            .waitDuration(resilience.getRetryWaitDuration())
                            .build());
        }

        @Bean
        @ConditionalOnMissingBean
        public Retry retry(RetryRegistry registry) {
            return registry.retry("rest-client");
        }

        @Bean
        @ConditionalOnMissingBean
        public RateLimiterRegistry rateLimiterRegistry(RestClientProperties properties) {
            var resilience = properties.getResilience();
            
            return RateLimiterRegistry.of(
                    io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                            .limitForPeriod(resilience.getLimitForPeriod())
                            .limitRefreshPeriod(resilience.getLimitRefreshPeriod())
                            .timeoutDuration(resilience.getTimeoutDuration())
                            .build());
        }

        @Bean
        @ConditionalOnMissingBean
        public RateLimiter rateLimiter(RateLimiterRegistry registry) {
            return registry.rateLimiter("rest-client");
        }
    }

    /**
     * Event consumer configuration
     */
    @Configuration
    @ConditionalOnProperty(name = "openfinance.rest-client.events.enabled", havingValue = "true")
    static class EventConsumerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnClass(JdbcTemplate.class)
        @ConditionalOnProperty(name = "openfinance.rest-client.events.source", havingValue = "database")
        public EventConsumer<?> databaseEventConsumer(
                EventConsumerProperties properties,
                JdbcTemplate jdbcTemplate,
                R2dbcEntityTemplate r2dbcTemplate,
                ObjectMapper objectMapper,
                MeterRegistry meterRegistry) {

            log.info("Creating DatabaseEventConsumer for table: {}", 
                    properties.getDatabase().getTableName());
            
            return new br.com.openfinance.restclient.events.consumer.DatabaseEventConsumer<>(
                    properties.toConfiguration(),
                    r2dbcTemplate,
                    jdbcTemplate,
                    objectMapper,
                    meterRegistry,
                    Object.class);
        }

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnClass(org.springframework.kafka.core.ConsumerFactory.class)
        @ConditionalOnProperty(name = "openfinance.rest-client.events.source", havingValue = "kafka")
        public EventConsumer<?> kafkaEventConsumer(
                EventConsumerProperties properties,
                ObjectMapper objectMapper,
                MeterRegistry meterRegistry) {

            log.info("Creating KafkaEventConsumer for topic: {}", 
                    properties.getKafka().getTopic());
            
            return new br.com.openfinance.restclient.events.consumer.KafkaEventConsumer<>(
                    properties.toConfiguration(),
                    objectMapper,
                    meterRegistry,
                    Object.class);
        }
    }

    /**
     * Alias bean for easier injection
     */
    @Bean
    @ConditionalOnMissingBean(name = "apiClient")
    public ApiClient<?> apiClient(
            @org.springframework.beans.factory.annotation.Autowired(required = false) ReactiveApiClient reactiveClient,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ImperativeApiClient imperativeClient,
            RestClientProperties properties) {

        if (properties.getStrategy() == RestClientStrategy.REACTIVE && reactiveClient != null) {
            return reactiveClient;
        } else if (properties.getStrategy() == RestClientStrategy.IMPERATIVE && imperativeClient != null) {
            return imperativeClient;
        } else if (reactiveClient != null) {
            return reactiveClient;
        } else if (imperativeClient != null) {
            return imperativeClient;
        } else {
            throw new IllegalStateException("No REST client implementation available. " +
                    "Please ensure you have either WebFlux or Web MVC dependencies in your classpath.");
        }
    }
}