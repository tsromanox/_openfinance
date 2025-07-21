package br.com.openfinance.accounts.infrastructure.config;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.CosmosClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("local")
@ConditionalOnProperty(name = "azure.cosmos.emulator.enabled", havingValue = "true", matchIfMissing = true)
public class LocalCosmosDbConfig {

    private static final String EMULATOR_ENDPOINT = "https://localhost:8081";
    private static final String EMULATOR_KEY = "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIuFJQ==";

    @Bean
    public CosmosAsyncClient cosmosAsyncClient() {
        log.info("Initializing Cosmos DB Emulator client for local development");

        return new CosmosClientBuilder()
                .endpoint(EMULATOR_ENDPOINT)
                .key(EMULATOR_KEY)
                .consistencyLevel(ConsistencyLevel.SESSION)
                .contentResponseOnWriteEnabled(true)
                .gatewayMode() // Emulador funciona melhor com Gateway mode
                .buildAsyncClient();
    }

    @Bean
    public CosmosAsyncDatabase cosmosAsyncDatabase(CosmosAsyncClient cosmosAsyncClient) {
        String databaseName = "openfinance-local";

        // Criar database no emulador
        cosmosAsyncClient.createDatabaseIfNotExists(databaseName)
                .doOnSuccess(response -> log.info("Local database {} is ready", databaseName))
                .block();

        return cosmosAsyncClient.getDatabase(databaseName);
    }
}