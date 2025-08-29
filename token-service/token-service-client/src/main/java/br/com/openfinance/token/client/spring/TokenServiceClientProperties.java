package br.com.openfinance.token.client.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "token.service.grpc")
public class TokenServiceClientProperties {

    private boolean enabled = true;
    private String host = "localhost";
    private int port = 9090;
    private boolean useTls = false;
    private Duration timeout = Duration.ofSeconds(10);
    private int maxRetries = 3;
    private boolean enableCache = true;
    private int cacheMaxSize = 1000;
    private Duration cacheTtl = Duration.ofMinutes(5);

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public boolean isUseTls() { return useTls; }
    public void setUseTls(boolean useTls) { this.useTls = useTls; }

    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public boolean isEnableCache() { return enableCache; }
    public void setEnableCache(boolean enableCache) { this.enableCache = enableCache; }

    public int getCacheMaxSize() { return cacheMaxSize; }
    public void setCacheMaxSize(int cacheMaxSize) { this.cacheMaxSize = cacheMaxSize; }

    public Duration getCacheTtl() { return cacheTtl; }
    public void setCacheTtl(Duration cacheTtl) { this.cacheTtl = cacheTtl; }
}
