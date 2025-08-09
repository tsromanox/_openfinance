package br.com.openfinance.core.infrastructure.http;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableRetry
public class RestClientConfig {

    @Value("${http.client.connection.timeout:30000}")
    private int connectionTimeout;

    @Value("${http.client.read.timeout:30000}")
    private int readTimeout;

    @Value("${http.client.max.connections:100}")
    private int maxConnections;

    @Value("${http.client.max.connections.per.route:20}")
    private int maxConnectionsPerRoute;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(this::clientHttpRequestFactory)

                //.setConnectTimeout(Duration.ofMillis(connectionTimeout))
                //.setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

    private HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        connectionManager.setDefaultSocketConfig(
                SocketConfig.custom()
                        .setSoTimeout(Timeout.ofMilliseconds(readTimeout))
                        .build()
        );

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .evictExpiredConnections()
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(connectionTimeout);
        factory.setConnectionRequestTimeout(connectionTimeout);

        return factory;
    }
}
