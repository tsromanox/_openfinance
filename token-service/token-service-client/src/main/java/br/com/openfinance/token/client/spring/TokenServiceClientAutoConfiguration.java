package br.com.openfinance.token.client.spring;

import br.com.openfinance.token.client.TokenServiceClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@ConditionalOnClass(TokenServiceClient.class)
@ConditionalOnProperty(prefix = "token.service.grpc", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TokenServiceClientProperties.class)
public class TokenServiceClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TokenServiceClient tokenServiceClient(TokenServiceClientProperties properties) {
        return TokenServiceClient.builder()
                .host(properties.getHost())
                .port(properties.getPort())
                .useTls(properties.isUseTls())
                .timeout(properties.getTimeout())
                .maxRetries(properties.getMaxRetries())
                .enableCache(properties.isEnableCache())
                .cacheMaxSize(properties.getCacheMaxSize())
                .cacheTtl(properties.getCacheTtl())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenServiceFacade tokenServiceFacade(TokenServiceClient client) {
        return new TokenServiceFacade(client);
    }
}
