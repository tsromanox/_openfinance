package br.com.openfinance.consents.infrastructure.adapter.persistence;

import br.com.openfinance.consents.domain.entity.Consent;
import br.com.openfinance.consents.domain.entity.ConsentStatus;
import br.com.openfinance.consents.domain.port.out.ConsentRepository;
import br.com.openfinance.consents.infrastructure.adapter.persistence.entity.ConsentEntity;
import br.com.openfinance.consents.infrastructure.adapter.persistence.mapper.ConsentMapper;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ConsentCosmosDbRepository implements ConsentRepository {

    private final CosmosAsyncContainer container;
    private final ConsentMapper mapper;

    @Value("${cosmos.db.consents.ttl:7776000}") // 90 days default
    private int defaultTtl;

    @Override
    public Mono<Consent> save(Consent consent) {
        ConsentEntity entity = mapper.toEntity(consent);
        entity.setId(consent.getConsentId());
        entity.setPartitionKey(consent.getClientId());
        entity.setTtl(calculateTtl(consent));
        entity.setLastModified(LocalDateTime.now());

        return container.upsertItem(entity)
                .map(response -> mapper.toDomain(response.getItem()))
                .doOnSuccess(c -> log.debug("Saved consent {} for client {}", c.getConsentId(), c.getClientId()))
                .doOnError(error -> log.error("Error saving consent {}", consent.getConsentId(), error));
    }

    @Override
    public Mono<Consent> findById(String consentId) {
        String query = "SELECT * FROM c WHERE c.id = @consentId";

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setQueryMetricsEnabled(true);

        return container.queryItems(query, options, ConsentEntity.class)
                .byPage(1)
                .flatMap(response -> Flux.fromIterable(response.getResults()))
                .next()
                .map(mapper::toDomain)
                .doOnNext(consent -> log.debug("Found consent {}", consentId));
    }

    @Override
    public Mono<Consent> findByIdAndClientId(String consentId, String clientId) {
        return container.readItem(consentId, new PartitionKey(clientId), ConsentEntity.class)
                .map(response -> mapper.toDomain(response.getItem()))
                .doOnNext(consent -> log.debug("Found consent {} for client {}", consentId, clientId))
                .onErrorResume(throwable -> {
                    log.debug("Consent {} not found for client {}", consentId, clientId);
                    return Mono.empty();
                });
    }

    @Override
    public Flux<Consent> findByClientId(String clientId) {
        String query = "SELECT * FROM c WHERE c.partitionKey = @clientId ORDER BY c.creationDateTime DESC";

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setPartitionKey(new PartitionKey(clientId));

        return container.queryItems(query, options, ConsentEntity.class)
                .byPage(100)
                .flatMap(response -> Flux.fromIterable(response.getResults()))
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Consent> findByStatus(ConsentStatus status) {
        String query = "SELECT * FROM c WHERE c.status = @status";

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setMaxDegreeOfParallelism(10);

        return container.queryItems(query, options, ConsentEntity.class)
                .byPage(1000)
                .flatMap(response -> Flux.fromIterable(response.getResults()))
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Consent> findByStatusAndExpirationDateTimeBefore(ConsentStatus status, LocalDateTime dateTime) {
        String query = "SELECT * FROM c WHERE c.status = @status AND c.expirationDateTime < @dateTime";

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setMaxDegreeOfParallelism(10);

        return container.queryItems(query, options, ConsentEntity.class)
                .byPage(1000)
                .flatMap(response -> Flux.fromIterable(response.getResults()))
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Consent> findActiveConsentsForClient(String clientId) {
        String query = """
            SELECT * FROM c 
            WHERE c.partitionKey = @clientId 
            AND c.status = 'AUTHORISED' 
            AND (c.expirationDateTime > @now OR c.expirationDateTime = null)
            ORDER BY c.creationDateTime DESC
            """;

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setPartitionKey(new PartitionKey(clientId));

        return container.queryItems(query, options, ConsentEntity.class)
                .byPage(100)
                .flatMap(response -> Flux.fromIterable(response.getResults()))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Long> countByClientIdAndStatus(String clientId, ConsentStatus status) {
        String query = "SELECT VALUE COUNT(1) FROM c WHERE c.partitionKey = @clientId AND c.status = @status";

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setPartitionKey(new PartitionKey(clientId));

        return container.queryItems(query, options, Long.class)
                .byPage(1)
                .flatMap(response -> Flux.fromIterable(response.getResults()))
                .next()
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Void> deleteById(String consentId) {
        return findById(consentId)
                .flatMap(consent -> container.deleteItem(
                        consentId,
                        new PartitionKey(consent.getClientId()),
                        new CosmosItemRequestOptions()
                ))
                .then()
                .doOnSuccess(v -> log.info("Deleted consent {}", consentId));
    }

    @Override
    public Flux<Consent> findConsentsToProcess(int limit) {
        String query = """
            SELECT TOP @limit * FROM c 
            WHERE c.status = 'AUTHORISED' 
            AND c.lastProcessedDateTime < @cutoffTime
            ORDER BY c.lastProcessedDateTime ASC
            """;

        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions();
        options.setMaxDegreeOfParallelism(10);

        return container.queryItems(query, options, ConsentEntity.class)
                .byPage(limit)
                .flatMap(response -> Flux.fromIterable(response.getResults()))
                .map(mapper::toDomain);
    }

    private int calculateTtl(Consent consent) {
        if (consent.getStatus() == ConsentStatus.REJECTED ||
                consent.getStatus() == ConsentStatus.REVOKED) {
            return 86400; // 1 day for rejected/revoked consents
        }

        if (consent.getExpirationDateTime() != null) {
            long secondsUntilExpiration = java.time.Duration.between(
                    LocalDateTime.now(),
                    consent.getExpirationDateTime()
            ).getSeconds();

            // Add 30 days after expiration for audit purposes
            return (int) Math.min(secondsUntilExpiration + 2592000, defaultTtl);
        }

        return defaultTtl;
    }
}
