package br.com.openfinance.restclient.events.consumer;

import br.com.openfinance.restclient.core.event.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.data.relational.core.query.Criteria.where;

/**
 * Event consumer that polls database tables for new events.
 * Supports both reactive (R2DBC) and imperative (JDBC) database access patterns.
 * 
 * Features:
 * - Configurable polling intervals
 * - Batch processing for performance
 * - Automatic retry handling for failed events
 * - Metrics collection
 * - Support for both reactive and blocking operations
 */
@Slf4j
public class DatabaseEventConsumer<T> implements EventConsumer<T> {

    private final EventConsumerConfiguration configuration;
    private final R2dbcEntityTemplate r2dbcTemplate;  // For reactive operations
    private final JdbcTemplate jdbcTemplate;          // For imperative operations
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Class<T> payloadType;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DatabaseEventConsumer(EventConsumerConfiguration configuration,
                               R2dbcEntityTemplate r2dbcTemplate,
                               JdbcTemplate jdbcTemplate,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry,
                               Class<T> payloadType) {
        this.configuration = configuration;
        this.r2dbcTemplate = r2dbcTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.payloadType = payloadType;
    }

    @Override
    public Flux<EventEnvelope<T>> consume() {
        if (!running.compareAndSet(false, true)) {
            return Flux.error(new IllegalStateException("Consumer is already running"));
        }

        log.info("Starting database event consumer for table: {}", 
                configuration.getDatabase().getTableName());

        if (configuration.getDatabase().isReactive()) {
            return consumeReactive();
        } else {
            return consumeImperative();
        }
    }

    /**
     * Reactive event consumption using R2DBC
     */
    private Flux<EventEnvelope<T>> consumeReactive() {
        return Flux.interval(configuration.getPollingInterval())
                .onBackpressureBuffer(configuration.getBufferSize())
                .flatMap(tick -> pollEventsReactive(), configuration.getBatchSize())
                .doOnSubscribe(sub -> meterRegistry.gauge("db_event_consumer_running", 1.0))
                .doFinally(sig -> {
                    running.set(false);
                    meterRegistry.gauge("db_event_consumer_running", 0.0);
                })
                .share(); // Share the stream among multiple subscribers
    }

    /**
     * Imperative event consumption using JDBC with Virtual Threads
     */
    private Flux<EventEnvelope<T>> consumeImperative() {
        return Flux.interval(configuration.getPollingInterval())
                .onBackpressureBuffer(configuration.getBufferSize())
                .flatMap(tick -> 
                    Mono.fromCallable(this::pollEventsImperative)
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMapMany(Flux::fromIterable),
                    configuration.getBatchSize())
                .doOnSubscribe(sub -> meterRegistry.gauge("db_event_consumer_running", 1.0))
                .doFinally(sig -> {
                    running.set(false);
                    meterRegistry.gauge("db_event_consumer_running", 0.0);
                })
                .share();
    }

    /**
     * Poll events using R2DBC (reactive)
     */
    private Mono<EventEnvelope<T>> pollEventsReactive() {
        String sql = buildSelectQuery();
        
        return r2dbcTemplate.getDatabaseClient()
                .sql(sql)
                .bind("limit", configuration.getBatchSize())
                .map(this::mapRowToEventEnvelope)
                .all()
                .cast(EventEnvelope.class)
                .flatMap(this::processEventReactive)
                .onErrorResume(this::handleError)
                .doOnNext(event -> 
                    meterRegistry.counter("db_events_consumed_total",
                            "table", configuration.getDatabase().getTableName(),
                            "status", "success").increment())
                .next(); // Take only the first event for this tick
    }

    /**
     * Poll events using JDBC (imperative)
     */
    private List<EventEnvelope<T>> pollEventsImperative() {
        String sql = buildSelectQuery();
        
        try {
            List<EventEnvelope<T>> events = jdbcTemplate.query(
                sql.replace(":limit", String.valueOf(configuration.getBatchSize())),
                new EventRowMapper());
                
            // Process each event (mark as processed)
            events.forEach(this::processEventImperative);
            
            meterRegistry.counter("db_events_consumed_total",
                    "table", configuration.getDatabase().getTableName(),
                    "status", "success").increment(events.size());
                    
            return events;
            
        } catch (DataAccessException e) {
            log.error("Error polling events from database", e);
            meterRegistry.counter("db_events_consumed_total",
                    "table", configuration.getDatabase().getTableName(),
                    "status", "error").increment();
            return List.of();
        }
    }

    /**
     * Process a single event reactively (mark as processed)
     */
    private Mono<EventEnvelope<T>> processEventReactive(EventEnvelope<T> event) {
        return markEventProcessedReactive(event)
                .thenReturn(event);
    }

    /**
     * Process a single event imperatively (mark as processed)
     */
    private void processEventImperative(EventEnvelope<T> event) {
        try {
            markEventProcessedImperative(event);
        } catch (DataAccessException e) {
            log.error("Failed to mark event as processed: {}", event.getEventId(), e);
            // Don't throw here, let the event be reprocessed
        }
    }

    /**
     * Mark event as processed using R2DBC
     */
    private Mono<Void> markEventProcessedReactive(EventEnvelope<T> event) {
        Query query = Query.query(where(configuration.getDatabase().getIdColumn()).is(event.getEventId()));
        Update update = Update.update(configuration.getDatabase().getProcessedColumn(), true)
                            .set("processed_at", OffsetDateTime.now());
        
        return r2dbcTemplate.update(query, update, configuration.getDatabase().getTableName())
                .then()
                .onErrorResume(throwable -> {
                    log.error("Failed to mark event as processed: {}", event.getEventId(), throwable);
                    return Mono.empty();
                });
    }

    /**
     * Mark event as processed using JDBC
     */
    private void markEventProcessedImperative(EventEnvelope<T> event) {
        String sql = String.format(
                "UPDATE %s SET %s = true, processed_at = CURRENT_TIMESTAMP WHERE %s = ?",
                configuration.getDatabase().getTableName(),
                configuration.getDatabase().getProcessedColumn(),
                configuration.getDatabase().getIdColumn());
        
        jdbcTemplate.update(sql, event.getEventId());
    }

    /**
     * Build SELECT query based on configuration
     */
    private String buildSelectQuery() {
        var dbConfig = configuration.getDatabase();
        
        return String.format(
                "SELECT * FROM %s WHERE %s %s LIMIT :limit",
                dbConfig.getTableName(),
                dbConfig.getWhereClause(),
                dbConfig.getOrderByClause().isEmpty() ? "" : "ORDER BY " + dbConfig.getOrderByClause());
    }

    /**
     * Map database row to EventEnvelope
     */
    private EventEnvelope<T> mapRowToEventEnvelope(io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata metadata) {
        try {
            String eventId = row.get(configuration.getDatabase().getIdColumn(), String.class);
            OffsetDateTime timestamp = row.get(configuration.getDatabase().getTimestampColumn(), OffsetDateTime.class);
            String payloadJson = row.get("payload", String.class);
            
            T payload = objectMapper.readValue(payloadJson, payloadType);
            
            return EventEnvelope.<T>builder()
                    .eventId(eventId)
                    .eventType(row.get("event_type", String.class))
                    .source(EventEnvelope.EventSource.DATABASE)
                    .timestamp(timestamp)
                    .payload(payload)
                    .correlationId(row.get("correlation_id", String.class))
                    .version(row.get("version", String.class))
                    .retryAttempt(row.get(configuration.getDatabase().getRetryCountColumn(), Integer.class))
                    .maxRetryAttempts(configuration.getMaxRetryAttempts())
                    .priority(EventEnvelope.EventPriority.valueOf(
                            row.get("priority", String.class)))
                    .targetEndpoint(row.get("target_endpoint", String.class))
                    .httpMethod(row.get("http_method", String.class))
                    .build();
                    
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize event payload", e);
        }
    }

    /**
     * JDBC RowMapper implementation
     */
    private class EventRowMapper implements RowMapper<EventEnvelope<T>> {
        @Override
        public EventEnvelope<T> mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                String payloadJson = rs.getString("payload");
                T payload = objectMapper.readValue(payloadJson, payloadType);
                
                return EventEnvelope.<T>builder()
                        .eventId(rs.getString(configuration.getDatabase().getIdColumn()))
                        .eventType(rs.getString("event_type"))
                        .source(EventEnvelope.EventSource.DATABASE)
                        .timestamp(rs.getTimestamp(configuration.getDatabase().getTimestampColumn())
                                    .toLocalDateTime().atOffset(ZoneOffset.UTC))
                        .payload(payload)
                        .correlationId(rs.getString("correlation_id"))
                        .version(rs.getString("version"))
                        .retryAttempt(rs.getInt(configuration.getDatabase().getRetryCountColumn()))
                        .maxRetryAttempts(configuration.getMaxRetryAttempts())
                        .priority(EventEnvelope.EventPriority.valueOf(rs.getString("priority")))
                        .targetEndpoint(rs.getString("target_endpoint"))
                        .httpMethod(rs.getString("http_method"))
                        .build();
                        
            } catch (JsonProcessingException e) {
                throw new SQLException("Failed to deserialize event payload", e);
            }
        }
    }

    /**
     * Handle errors during event processing
     */
    private Mono<EventEnvelope<T>> handleError(Throwable throwable) {
        log.error("Error processing database event", throwable);
        meterRegistry.counter("db_events_consumed_total",
                "table", configuration.getDatabase().getTableName(),
                "status", "error").increment();
        return Mono.empty();
    }

    @Override
    public void stop() {
        log.info("Stopping database event consumer");
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public EventConsumerConfiguration getConfiguration() {
        return configuration;
    }
}