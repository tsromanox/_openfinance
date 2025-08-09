package br.com.openfinance.consents.infrastructure.adapter.messaging;

import br.com.openfinance.consents.domain.entity.ConsentEvent;
import br.com.openfinance.consents.domain.port.out.ConsentEventPublisher;
import br.com.openfinance.consents.dto.ConsentUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentKafkaPublisher implements ConsentEventPublisher {

    private final ReactiveKafkaProducerTemplate<String, Object> kafkaProducerTemplate;

    private static final String CONSENT_EVENTS_TOPIC = "consent-events";
    private static final String CONSENT_UPDATES_TOPIC = "consent-updates";

    @Override
    public Mono<Void> publishConsentEvent(ConsentEvent event) {
        return kafkaProducerTemplate.send(CONSENT_EVENTS_TOPIC, event.getConsentId(), event)
                .doOnSuccess(result -> log.debug("Published consent event {} for consent {}",
                        event.getType(), event.getConsentId()))
                .doOnError(error -> log.error("Error publishing consent event", error))
                .then();
    }

    @Override
    public Mono<Void> publishConsentUpdate(String consentId, ConsentUpdateEvent updateEvent) {
        return kafkaProducerTemplate.send(CONSENT_UPDATES_TOPIC, consentId, updateEvent)
                .doOnSuccess(result -> log.debug("Published consent update for consent {}", consentId))
                .doOnError(error -> log.error("Error publishing consent update", error))
                .then();
    }
}
