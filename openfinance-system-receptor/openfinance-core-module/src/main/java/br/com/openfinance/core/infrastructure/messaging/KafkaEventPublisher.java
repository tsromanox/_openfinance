package br.com.openfinance.core.infrastructure.messaging;

import br.com.openfinance.core.application.ports.EventPublisher;
import br.com.openfinance.core.domain.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent event) {
        try {
            String topicName = getTopicName(event);
            String eventJson = objectMapper.writeValueAsString(event);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(topicName, event.getAggregateId(), eventJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Event published successfully: {} to topic: {}",
                            event.getClass().getSimpleName(), topicName);
                } else {
                    log.error("Failed to publish event: {}",
                            event.getClass().getSimpleName(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    @Override
    public void publishAsync(DomainEvent event) {
        try {
            String topicName = getTopicName(event);
            String eventJson = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(topicName, event.getAggregateId(), eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Async event published: {} to topic: {}",
                                    event.getClass().getSimpleName(), topicName);
                        } else {
                            log.error("Async publish failed for event: {}",
                                    event.getClass().getSimpleName(), ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Error in async publishing event", e);
            // Optionally, do not throw to keep async contract
        }
    }

    private String getTopicName(DomainEvent event) {
        // Convert event class name to topic name
        // e.g., ConsentAuthorizedEvent -> consent-authorized
        String className = event.getClass().getSimpleName();
        return className.replaceAll("Event$", "")
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
    }
}
