package com.bank.openfinance.client.event.source;

/**
 * Enumeration of available event source types.
 */
public enum EventSourceType {
    DATABASE("Database"),
    KAFKA("Kafka"),
    RABBITMQ("RabbitMQ"),
    SQS("AWS SQS"),
    AZURE_SERVICE_BUS("Azure Service Bus"),
    FILE("File"),
    HTTP("HTTP/Webhook"),
    CUSTOM("Custom");

    private final String displayName;

    EventSourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
