package br.com.openfinance.infrastructure.client.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for WebClient optimized for parallel processing.
 */
@Configuration
public class WebClientConfig {
    
    @Bean
    @ConfigurationProperties(prefix = "openfinance.client")
    public OpenFinanceClientProperties openFinanceClientProperties() {
        return new OpenFinanceClientProperties();
    }
    
    @Bean("openFinanceWebClient")
    public WebClient openFinanceWebClient(OpenFinanceClientProperties properties) {
        // Configure connection provider for high concurrency
        ConnectionProvider connectionProvider = ConnectionProvider.builder("openfinance-pool")
                .maxConnections(properties.getMaxConnections())
                .maxIdleTime(Duration.ofSeconds(properties.getMaxIdleTimeSeconds()))
                .maxLifeTime(Duration.ofSeconds(properties.getMaxLifeTimeSeconds()))
                .pendingAcquireTimeout(Duration.ofSeconds(properties.getPendingAcquireTimeoutSeconds()))
                .evictInBackground(Duration.ofSeconds(30))
                .build();
        
        // Configure HTTP client with optimized settings
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMillis())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(properties.getWriteTimeoutSeconds(), TimeUnit.SECONDS)))
                .compress(true)
                .followRedirect(true);
        
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    // Increase buffer size for large responses
                    configurer.defaultCodecs().maxInMemorySize(properties.getMaxBufferSizeBytes());
                })
                .build();
    }
    
    /**
     * Configuration properties for OpenFinance client.
     */
    public static class OpenFinanceClientProperties {
        private String baseUrl = "https://api.openfinance.org.br";
        private int maxConnections = 200;
        private int maxIdleTimeSeconds = 20;
        private int maxLifeTimeSeconds = 60;
        private int pendingAcquireTimeoutSeconds = 45;
        private int connectTimeoutMillis = 10000;
        private int readTimeoutSeconds = 30;
        private int writeTimeoutSeconds = 30;
        private int maxBufferSizeBytes = 16 * 1024 * 1024; // 16MB
        
        // Getters and setters
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public int getMaxConnections() { return maxConnections; }
        public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
        
        public int getMaxIdleTimeSeconds() { return maxIdleTimeSeconds; }
        public void setMaxIdleTimeSeconds(int maxIdleTimeSeconds) { this.maxIdleTimeSeconds = maxIdleTimeSeconds; }
        
        public int getMaxLifeTimeSeconds() { return maxLifeTimeSeconds; }
        public void setMaxLifeTimeSeconds(int maxLifeTimeSeconds) { this.maxLifeTimeSeconds = maxLifeTimeSeconds; }
        
        public int getPendingAcquireTimeoutSeconds() { return pendingAcquireTimeoutSeconds; }
        public void setPendingAcquireTimeoutSeconds(int pendingAcquireTimeoutSeconds) { this.pendingAcquireTimeoutSeconds = pendingAcquireTimeoutSeconds; }
        
        public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
        public void setConnectTimeoutMillis(int connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }
        
        public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
        public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
        
        public int getWriteTimeoutSeconds() { return writeTimeoutSeconds; }
        public void setWriteTimeoutSeconds(int writeTimeoutSeconds) { this.writeTimeoutSeconds = writeTimeoutSeconds; }
        
        public int getMaxBufferSizeBytes() { return maxBufferSizeBytes; }
        public void setMaxBufferSizeBytes(int maxBufferSizeBytes) { this.maxBufferSizeBytes = maxBufferSizeBytes; }
    }
}