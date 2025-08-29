package com.bank.openfinance.client.event.source;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Health indicator for event sources.
 */
@Component
@RequiredArgsConstructor
public class EventSourceHealthIndicator implements HealthIndicator {

    private final EventSourceManager eventSourceManager;

    @Override
    public Health health() {
        try {
            Health.Builder builder = new Health.Builder();

            boolean allHealthy = eventSourceManager.areAllSourcesHealthy();

            if (allHealthy) {
                builder.up();
            } else {
                builder.down();
            }

            // Add status details for each source
            eventSourceManager.getAllStatuses().forEach((name, status) -> {
                builder.withDetail(name, Map.of(
                        "type", status.getSourceType().getDisplayName(),
                        "healthy", status.isHealthy(),
                        "eventsConsumed", status.getEventsConsumed() != null ? status.getEventsConsumed() : 0,
                        "eventsProcessed", status.getEventsProcessed() != null ? status.getEventsProcessed() : 0,
                        "eventsFailed", status.getEventsFailed() != null ? status.getEventsFailed() : 0,
                        "state", status.getCurrentState() != null ? status.getCurrentState() : "UNKNOWN"
                ));
            });

            return builder.build();

        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}
