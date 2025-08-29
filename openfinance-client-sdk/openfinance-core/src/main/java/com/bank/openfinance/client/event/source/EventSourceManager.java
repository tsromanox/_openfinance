package com.bank.openfinance.client.event.source;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages lifecycle of event sources.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "openfinance.event.manager.enabled", havingValue = "true", matchIfMissing = true)
public class EventSourceManager {

    private final EventSourceFactory eventSourceFactory;
    private final Map<String, EventSource> activeSources = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        log.info("Initializing EventSourceManager");

        // Create and start the primary event source
        EventSource primarySource = eventSourceFactory.createEventSource();
        registerAndStart("primary", primarySource);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down EventSourceManager");

        // Stop all active sources
        activeSources.forEach((name, source) -> {
            try {
                log.info("Stopping event source: {}", name);
                source.stop();
            } catch (Exception e) {
                log.error("Error stopping event source {}: {}", name, e.getMessage(), e);
            }
        });

        activeSources.clear();
    }

    /**
     * Register and start an event source.
     *
     * @param name Name to identify the source
     * @param source The event source to register
     */
    public void registerAndStart(String name, EventSource source) {
        if (activeSources.containsKey(name)) {
            throw new IllegalArgumentException("Event source already registered: " + name);
        }

        activeSources.put(name, source);
        source.start();
        log.info("Registered and started event source: {} of type: {}",
                name, source.getSourceType());
    }

    /**
     * Get an active event source by name.
     *
     * @param name Name of the source
     * @return EventSource if found, null otherwise
     */
    public EventSource getSource(String name) {
        return activeSources.get(name);
    }

    /**
     * Get the primary event source.
     *
     * @return Primary EventSource
     */
    public EventSource getPrimarySource() {
        return activeSources.get("primary");
    }

    /**
     * Get all active event sources.
     *
     * @return List of active sources
     */
    public List<EventSource> getAllSources() {
        return new ArrayList<>(activeSources.values());
    }

    /**
     * Get status of all event sources.
     *
     * @return Map of source names to their status
     */
    public Map<String, EventSourceStatus> getAllStatuses() {
        Map<String, EventSourceStatus> statuses = new ConcurrentHashMap<>();
        activeSources.forEach((name, source) ->
                statuses.put(name, source.getStatus()));
        return statuses;
    }

    /**
     * Check if all sources are healthy.
     *
     * @return true if all sources are healthy
     */
    public boolean areAllSourcesHealthy() {
        return activeSources.values().stream()
                .allMatch(EventSource::isHealthy);
    }

    /**
     * Restart a specific event source.
     *
     * @param name Name of the source to restart
     */
    public void restartSource(String name) {
        EventSource source = activeSources.get(name);
        if (source != null) {
            log.info("Restarting event source: {}", name);
            source.stop();
            source.start();
        } else {
            log.warn("Event source not found for restart: {}", name);
        }
    }
}
