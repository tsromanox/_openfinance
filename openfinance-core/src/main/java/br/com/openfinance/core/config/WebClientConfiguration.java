package br.com.openfinance.core.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient configuration for OpenFinance services
 * Provides a pre-configured WebClient builder with optimal settings
 */
@Slf4j
@Configuration
public class WebClientConfiguration {

    @Value("${openfinance.webclient.timeout:30}")
    private int timeoutSeconds;

    @Value("${openfinance.webclient.max-in-memory-size:10485760}") // 10MB
    private int maxInMemorySize;

    @Value("${openfinance.webclient.max-connections:500}")
    private int maxConnections;

    @Value("${openfinance.webclient.max-pending-acquires:1000}")
    private int maxPendingAcquires;

    @Value("${openfinance.webclient.ssl.trust-all:false}")
    private boolean trustAllSsl;

    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder() throws SSLException {
        // Connection provider with pooling
        ConnectionProvider connectionProvider = ConnectionProvider.builder("openfinance-pool")
                .maxConnections(maxConnections)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .pendingAcquireMaxCount(maxPendingAcquires)
                .evictInBackground(Duration.ofSeconds(30))
                .build();

        // SSL Context
        SslContext sslContext = createSslContext();

        // HTTP Client configuration
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .secure(spec -> spec.sslContext(sslContext))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS)))
                .compress(true)
                .followRedirect(false);

        // Exchange strategies for large payloads
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(this::configureCodecs)
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse());
    }

    private void configureCodecs(ClientCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(maxInMemorySize);
        configurer.defaultCodecs().enableLoggingRequestDetails(true);
    }

    private SslContext createSslContext() throws SSLException {
        SslContextBuilder builder = SslContextBuilder.forClient();

        if (trustAllSsl) {
            // Only for development/testing - NOT for production
            builder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }

        return builder.build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                clientRequest.headers().forEach((name, values) ->
                        values.forEach(value -> log.debug("{}: {}", name, value)));
            }
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("Response status: {}", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }
}
