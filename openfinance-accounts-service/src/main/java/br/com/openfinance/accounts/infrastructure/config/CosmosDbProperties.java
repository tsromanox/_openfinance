package br.com.openfinance.accounts.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "azure.cosmos")
public class CosmosDbProperties {

    @NotBlank
    private String endpoint;

    @NotBlank
    private String key;

    @NotBlank
    private String database;

    @NotNull
    private String consistencyLevel = "SESSION";

    @NotNull
    private String connectionMode = "DIRECT";

    @Positive
    private int maxRetryAttempts = 9;

    @Positive
    private int maxRetryWaitTime = 30;

    private List<String> preferredRegions = List.of("Brazil South", "East US");

    @Data
    public static class Container {
        private String accounts = "accounts";
        private int defaultTtlDays = 30;
        private int autoscaleMaxThroughput = 4000;
    }

    private Container container = new Container();
}
