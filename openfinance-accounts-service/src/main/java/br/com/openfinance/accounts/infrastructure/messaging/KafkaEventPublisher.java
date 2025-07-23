// Exemplo de uso do ReactiveKafkaProducerTemplate em um service
// KafkaEventPublisher.java
package br.com.openfinance.accounts.infrastructure.messaging;

import br.com.openfinance.accounts.application.event.AccountUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final ReactiveKafkaProducerTemplate<String, AccountUpdateEvent> kafkaTemplate;

    public Mono<SenderResult<Void>> publishAccountUpdate(AccountUpdateEvent event) {
        return kafkaTemplate.send("account-updates", event.getAccountId(), event)
                .doOnSuccess(result -> {
                    log.debug("Event published successfully for account: {}, offset: {}",
                            event.getAccountId(), result.recordMetadata().offset());
                })
                .doOnError(error -> {
                    log.error("Failed to publish event for account: {}",
                            event.getAccountId(), error);
                });
    }

    public Mono<Void> publishAccountUpdateWithRetry(AccountUpdateEvent event) {
        return publishAccountUpdate(event)
                .retry(3)
                .onErrorResume(error -> {
                    // Enviar para DLQ em caso de falha
                    return kafkaTemplate.send("account-updates-dlq", event.getAccountId(), event)
                            .doOnSuccess(r -> log.warn("Event sent to DLQ for account: {}",
                                    event.getAccountId()))
                            .then(Mono.empty());
                })
                .then();
    }
}
