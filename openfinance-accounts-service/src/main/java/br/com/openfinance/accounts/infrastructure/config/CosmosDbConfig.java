package br.com.openfinance.accounts.infrastructure.config;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.ThroughputProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "azure.cosmos.enabled", havingValue = "true", matchIfMissing = true)
public class CosmosDbConfig {

    @Value("${azure.cosmos.endpoint}")
    private String endpoint;

    @Value("${azure.cosmos.key}")
    private String key;

    @Value("${azure.cosmos.database}")
    private String databaseName;

    @Value("${azure.cosmos.consistency-level:SESSION}")
    private String consistencyLevel;

    @Value("${azure.cosmos.preferred-regions:Brazil South,East US}")
    private String preferredRegions;

    @Value("${azure.cosmos.connection-mode:DIRECT}")
    private String connectionMode;

    @Bean
    public CosmosAsyncClient cosmosAsyncClient() {
        log.info("Initializing Cosmos DB client for endpoint: {}", endpoint);

        CosmosClientBuilder builder = new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key)
                .consistencyLevel(getConsistencyLevel())
                .contentResponseOnWriteEnabled(true);

        // Configurar regiões preferenciais
        if (!preferredRegions.isEmpty()) {
            List<String> regions = Arrays.asList(preferredRegions.split(","));
            builder.preferredRegions(regions);
        }

        // Usar modo direto para melhor performance
        if ("DIRECT".equalsIgnoreCase(connectionMode)) {
            builder.directMode();
        } else {
            builder.gatewayMode();
        }

        CosmosAsyncClient client = builder.buildAsyncClient();

        // Inicializar database e containers
        initializeCosmosDb(client);

        return client;
    }

    @Bean
    public CosmosAsyncDatabase cosmosAsyncDatabase(CosmosAsyncClient cosmosAsyncClient) {
        return cosmosAsyncClient.getDatabase(databaseName);
    }

    private void initializeCosmosDb(CosmosAsyncClient client) {
        // Criar database se não existir
        client.createDatabaseIfNotExists(databaseName)
                .doOnSuccess(response -> {
                    log.info("Database {} is ready", databaseName);
                    createContainers(client);
                })
                .doOnError(error -> log.error("Failed to create database", error))
                .block(Duration.ofSeconds(30));
    }

    private void createContainers(CosmosAsyncClient client) {
        CosmosAsyncDatabase database = client.getDatabase(databaseName);

        // Container de accounts
        createAccountsContainer(database);
    }

    private void createAccountsContainer(CosmosAsyncDatabase database) {
        String containerName = "accounts";

        try {
            // Criar container com partition key simples
            CosmosContainerProperties containerProperties =
                    new CosmosContainerProperties(containerName, "/partitionKey");

            // TTL padrão de 30 dias
            containerProperties.setDefaultTimeToLiveInSeconds(30 * 24 * 60 * 60);

            // Throughput com autoscale
            ThroughputProperties throughput = ThroughputProperties.createAutoscaledThroughput(4000);

            database.createContainerIfNotExists(containerProperties, throughput)
                    .doOnSuccess(response ->
                            log.info("Container {} is ready", containerName))
                    .doOnError(error ->
                            log.error("Failed to create container {}", containerName, error))
                    .block(Duration.ofSeconds(30));

        } catch (Exception e) {
            log.error("Error creating container {}", containerName, e);
        }
    }

    private ConsistencyLevel getConsistencyLevel() {
        try {
            return ConsistencyLevel.valueOf(consistencyLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid consistency level: {}, using SESSION", consistencyLevel);
            return ConsistencyLevel.SESSION;
        }
    }
}