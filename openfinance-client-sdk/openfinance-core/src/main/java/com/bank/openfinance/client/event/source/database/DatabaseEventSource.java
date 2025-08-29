package com.bank.openfinance.client.event.source.database;

import com.bank.openfinance.client.event.model.EventEnvelope;
import com.bank.openfinance.client.event.source.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Database-based event source implementation.
 * Polls events from a database table and supports both blocking and reactive consumption.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "openfinance.event.source", havingValue = "database")
public class DatabaseEventSource extends AbstractEventSource implements PollableEventSource, BatchEventSource {

    private final EventRepository repository;
    private final DatabaseEventConfiguration config;
    private final ObjectMapper objectMapper;

    private ScheduledExecutorService scheduler;
    private ExecutorService virtualThreadExecutor;
    private final BlockingQueue<EventEnvelope<?>> eventQueue;
    private final AtomicReference<String> lastError = new AtomicReference<>();
    private final Map<UUID, CompletableFuture<Void>> pendingAcks = new ConcurrentHashMap<>();

    public DatabaseEventSource(EventRepository repository,
                               DatabaseEventConfiguration config,
                               ObjectMapper objectMapper) {
        super();
        this.repository = repository;
        this.config = config;
        this.objectMapper = objectMapper;
        this.eventQueue = new LinkedBlockingQueue<>(config.getQueueCapacity());
    }

    @Override
    protected void doStart() {
        // Initialize executors
        if (config.isUseVirtualThreads()) {
            virtualThreadExecutor = Executors.newThreadPerTaskExecutor(
                    Thread.ofVirtual()
                            .name("db-event-vthread-", 0)
                            .factory()
            );
            log.info("Database event source using Virtual Threads");
        } else {
            virtualThreadExecutor = Executors.newFixedThreadPool(
                    config.getThreadPoolSize(),
                    r -> {
                        Thread t = new Thread(r);
                        t.setName("db-event-thread-" + t.getId());
                        t.setDaemon(true);
                        return t;
                    }
            );
            log.info("Database event source using fixed thread pool of size {}",
                    config.getThreadPoolSize());
        }

        // Start polling scheduler
        scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r);
            t.setName("db-event-scheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleWithFixedDelay(
                this::pollDatabase,
                0,
                config.getPollInterval(),
                TimeUnit.MILLISECONDS
        );

        log.info("Database event source started with poll interval: {}ms",
                config.getPollInterval());
    }

    @Override
    protected void doStop() {

    }

    @Override
    protected boolean doHealthCheck() {
        return false;
    }

    @Override
    protected Map<String, Object> getStatusMetadata() {
        return Map.of();
    }

    @Override
    protected String getLastError() {
        return "";
    }

    @Override
    protected void doAcknowledge(EventEnvelope<?> envelope) {

    }

    @Override
    protected void doReportError(EventEnvelope<?> envelope, Throwable error) {

    }

    @Override
    public Optional<EventEnvelope<?>> poll(Duration timeout) {
        return Optional.empty();
    }

    @Override
    public List<EventEnvelope<?>> pollBatch(int maxEvents, Duration timeout) {
        return List.of();
    }

    @Override
    public void setPollingInterval(Duration interval) {

    }

    @Override
    public Stream<EventEnvelope<?>> consumeBlocking() {
        return Stream.empty();
    }

    @Override
    public Flux<EventEnvelope<?>> consumeReactive() {
        return null;
    }

    @Override
    public EventSourceType getSourceType() {
        return null;
    }

    @Override
    public Stream<List<EventEnvelope<?>>> consumeBatchBlocking(int batchSize, Duration timeout) {
        return Stream.empty();
    }

    @Override
    public Flux<List<EventEnvelope<?>>> consumeBatchReactive(int batchSize, Duration timeout) {
        return null;
    }

    @Override
    public void acknowledgeBatch(List<EventEnvelope<?>> envelopes) {

    }

    @Override
    public void reportBatchErrors(List<EventProcessingFailure> failures) {

    }

    // ===== Private Methods =====

    private void pollDatabase() {
        if (!running.get()) return;

        try {
            // Use Virtual Threads for parallel processing if enabled
            if (config.isUseVirtualThreads()) {
                pollWithVirtualThreads();
            } else {
                pollWithTraditionalThreads();
            }
        } catch (Exception e) {
            log.error("Error polling database", e);
            lastError.set(e.getMessage());
        }
    }

    private void pollWithVirtualThreads() {
        List<EventEntity> entities = fetchUnprocessedEvents();

        if (entities.isEmpty()) return;

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            List<StructuredTaskScope.Subtask<EventEnvelope<?>>> tasks = entities.stream()
                    .map(entity -> scope.fork(() -> processEntity(entity)))
                    //.collect(Collectors.toList())
                    .toList();
            

            scope.join();
            scope.throwIfFailed();

            // Add successfully processed events to queue
            for (var task : tasks) {
                EventEnvelope<?> envelope = task.get();
                if (envelope != null) {
                    offerToQueue(envelope);
                }
            }

            log.debug("Processed {} events with virtual threads", entities.size());

        } catch (Exception e) {
            log.error("Error in virtual thread processing", e);
        }
    }

    private void pollWithTraditionalThreads() {
        List<EventEntity> entities = fetchUnprocessedEvents();

        if (entities.isEmpty()) return;

        List<CompletableFuture<EventEnvelope<?>>> futures = entities.stream()
                .map(entity -> CompletableFuture.supplyAsync(
                        () -> processEntity(entity),
                        virtualThreadExecutor
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .forEach(this::offerToQueue);

                    log.debug("Processed {} events with thread pool", entities.size());
                })
                .exceptionally(e -> {
                    log.error("Error in traditional thread processing", e);
                    return null;
                });
    }

    private List<EventEntity> fetchUnprocessedEvents() {
        PageRequest pageRequest = PageRequest.of(
                0,
                config.getBatchSize(),
                Sort.by(Sort.Direction.ASC, "priority", "createdAt")
        );

        return repository.findUnprocessedEvents(
                Arrays.asList(EventEntity.EventStatus.PENDING, EventEntity.EventStatus.RETRY),
                Instant.now(),
                pageRequest
        );
    }

    private EventEnvelope<?> processEntity(EventEntity entity) {
        try {
            // Mark as processing
            entity.setStatus(EventEntity.EventStatus.PROCESSING);
            entity.setProcessingStartedAt(Instant.now());
            repository.save(entity);

            // Create envelope
            return mapToEnvelope(entity);

        } catch (Exception e) {
            log.error("Error processing entity {}", entity.getId(), e);
            handleEntityError(entity, e);
            return null;
        }
    }

    private EventEnvelope<?> mapToEnvelope(EventEntity entity) {
        return EventEnvelope.builder()
                .eventId(entity.getId())
                .eventType(entity.getEventType())
                .source("database")
                .timestamp(entity.getCreatedAt())
                .correlationId(entity.getCorrelationId())
                .version(entity.getVersion() != null ? entity.getVersion().intValue() : 1)
                .payload(entity.getPayload())
                .headers(entity.getHeaders())
                .tenantId(entity.getTenantId())
                .userId(entity.getUserId())
                .retryCount(entity.getRetryCount())
                .processAfter(entity.getProcessAfter())
                .priority(EventEnvelope.EventPriority.valueOf(
                        entity.getPriority() != null ? entity.getPriority() : "NORMAL"
                ))
                .build();
    }

    private void offerToQueue(EventEnvelope<?> envelope) {
        try {
            if (!eventQueue.offer(envelope, 1, TimeUnit.SECONDS)) {
                log.warn("Event queue is full, dropping event: {}", envelope.getEventId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while offering to queue", e);
        }
    }

    private void handleEntityError(EventEntity entity, Exception error) {
        entity.setStatus(EventEntity.EventStatus.RETRY);
        entity.setLastError(error.getMessage());
        entity.setLastErrorAt(Instant.now());
        entity.setRetryCount(entity.getRetryCount() + 1);

        if (entity.getRetryCount() >= config.getMaxRetries()) {
            entity.setStatus(EventEntity.EventStatus.FAILED);
        } else {
            long backoffSeconds = (long) Math.pow(2, entity.getRetryCount()) * 60;
            entity.setProcessAfter(Instant.now().plusSeconds(backoffSeconds));
        }

        repository.save(entity);
    }
}
