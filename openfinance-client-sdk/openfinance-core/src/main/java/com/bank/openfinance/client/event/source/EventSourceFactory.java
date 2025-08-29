package com.bank.openfinance.client.event.source;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Factory for creating event sources based on configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventSourceFactory {

    private final ApplicationContext applicationContext;

    @Value("${openfinance.event.source:database}")
    private String eventSourceType;

    /**
     * Create an event source based on the configured type.
     *
     * @return EventSource implementation
     */
    public EventSource createEventSource() {
        log.info("Creating event source of type: {}", eventSourceType);

        return switch (eventSourceType.toLowerCase()) {
            case "database", "db" -> applicationContext.getBean("databaseEventSource", EventSource.class);
            case "kafka" -> applicationContext.getBean("kafkaEventSource", EventSource.class);
            case "rabbitmq" -> createRabbitMQEventSource();
            case "sqs" -> createSQSEventSource();
            case "file" -> createFileEventSource();
            default -> throw new IllegalArgumentException(
                    "Unknown event source type: " + eventSourceType);
        };
    }

    /**
     * Create an event source of a specific type.
     *
     * @param type The type of event source to create
     * @return EventSource implementation
     */
    public EventSource createEventSource(EventSourceType type) {
        log.info("Creating event source of type: {}", type);

        return switch (type) {
            case DATABASE -> applicationContext.getBean("databaseEventSource", EventSource.class);
            case KAFKA -> applicationContext.getBean("kafkaEventSource", EventSource.class);
            case RABBITMQ -> createRabbitMQEventSource();
            case SQS -> createSQSEventSource();
            case FILE -> createFileEventSource();
            default -> throw new IllegalArgumentException(
                    "Unsupported event source type: " + type);
        };
    }

    private EventSource createRabbitMQEventSource() {
        // Placeholder for RabbitMQ implementation
        throw new UnsupportedOperationException("RabbitMQ event source not yet implemented");
    }

    private EventSource createSQSEventSource() {
        // Placeholder for SQS implementation
        throw new UnsupportedOperationException("SQS event source not yet implemented");
    }

    private EventSource createFileEventSource() {
        // Placeholder for File implementation
        throw new UnsupportedOperationException("File event source not yet implemented");
    }
}
