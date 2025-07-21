package br.com.openfinance.accounts.infrastructure.repository;

import br.com.openfinance.accounts.domain.entity.Account;
import br.com.openfinance.accounts.domain.port.AccountRepository;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
public class CosmosDbAccountRepository implements AccountRepository {

    private final CosmosAsyncContainer container;

    public CosmosDbAccountRepository(
            CosmosAsyncClient cosmosClient,
            @Value("${azure.cosmos.database}") String databaseName,
            @Value("${azure.cosmos.container.accounts}") String containerName) {

        this.container = cosmosClient
                .getDatabase(databaseName)
                .getContainer(containerName);
    }

    @Override
    public Mono<Account> save(Account account) {
        return container.upsertItem(account)
                .map(response -> response.getItem())
                .doOnSuccess(saved -> log.debug("Account {} saved successfully", saved.getId()))
                .doOnError(error -> log.error("Error saving account {}", account.getId(), error));
    }

    @Override
    public Mono<Account> findById(String id) {
        return container.readItem(id, new PartitionKey(id), Account.class)
                .map(response -> response.getItem())
                .doOnError(error -> log.error("Error finding account by id {}", id, error));
    }

    @Override
    public Flux<Account> findByClientId(String clientId) {
        String query = "SELECT * FROM c WHERE c.clientId = @clientId";

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setQueryMetricsEnabled(true);

        return container.queryItems(query, options, Account.class)
                .byPage()
                .flatMap(response -> Flux.fromIterable(response.getResults()));
    }

    @Override
    public Flux<Account> findByConsentId(String consentId) {
        String query = "SELECT * FROM c WHERE c.consentId = @consentId";

        return container.queryItems(query, new CosmosQueryRequestOptions(), Account.class)
                .byPage()
                .flatMap(response -> Flux.fromIterable(response.getResults()));
    }

    @Override
    public Flux<Account> findAccountsForUpdate(int limit) {
        String query = """
            SELECT * FROM c 
            WHERE c.status = 'ACTIVE' 
            AND (c.lastUpdated < DateTimeAdd('hh', -12, GetCurrentDateTime()) 
                 OR c.lastUpdated = null)
            ORDER BY c.lastUpdated ASC
            OFFSET 0 LIMIT @limit
            """;

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setMaxBufferedItemCount(limit);

        return container.queryItems(query, options, Account.class)
                .byPage()
                .flatMap(response -> Flux.fromIterable(response.getResults()));
    }

    @Override
    public Mono<Long> countByClientId(String clientId) {
        String query = "SELECT VALUE COUNT(1) FROM c WHERE c.clientId = @clientId";

        return container.queryItems(query, new CosmosQueryRequestOptions(), Long.class)
                .byPage()
                .next()
                .map(response -> response.getResults().get(0));
    }
}
