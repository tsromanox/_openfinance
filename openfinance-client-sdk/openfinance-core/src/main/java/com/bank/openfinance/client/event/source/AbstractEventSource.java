package com.bank.openfinance.client.event.source;

import com.bank.openfinance.client.event.model.EventEnvelope;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Abstract base class for event sources providing common functionality.
 */
@Slf4j
public abstract class AbstractEventSource implements EventSource {

    protected final AtomicBoolean running = new AtomicBoolean(false);
    protected final AtomicLong eventsConsumed = new AtomicLong(0);
    protected final AtomicLong eventsProcessed = new AtomicLong(0);
    protected final AtomicLong eventsFailed = new AtomicLong(0);
    protected Instant startTime;
    protected Instant lastEventTime;

    @Autowired(required = false)
    protected MeterRegistry meterRegistry;

    protected Counter consumedCounter;
    protected Counter processedCounter;
    protected Counter failedCounter;
    protected Timer processingTimer;

    // Reactive sink for bridging blocking to reactive
    protected final Sinks.Many<EventEnvelope<?>> eventSink =
            Sinks.many().multicast().onBackpressureBuffer(10000);

    protected AbstractEventSource() {
        initializeMetrics();
    }

    protected void initializeMetrics() {
        if (meterRegistry != null) {
            String sourceType = getSourceType().name().toLowerCase();

            consumedCounter = Counter.builder("events.consumed")
                    .tag("source", sourceType)
                    .description("Total events consumed from source")
                    .register(meterRegistry);

            processedCounter = Counter.builder("events.processed")
                    .tag("source", sourceType)
                    .description("Total events successfully processed")
                    .register(meterRegistry);

            failedCounter = Counter.builder("events.failed")
                    .tag("source", sourceType)
                    .description("Total events that failed processing")
                    .register(meterRegistry);

            processingTimer = Timer.builder("events.processing.time")
                    .tag("source", sourceType)
                    .description("Event processing time")
                    .register(meterRegistry);
        }
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            startTime = Instant.now();
            log.info("Starting {} event source", getSourceType());
            doStart();
        } else {
            log.warn("{} event source is already running", getSourceType());
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("Stopping {} event source", getSourceType());
            doStop();
            eventSink.tryEmitComplete();
        } else {
            log.warn("{} event source is not running", getSourceType());
        }
    }

    @Override
    public boolean isHealthy() {
        return running.get() && doHealthCheck();
    }

    @Override
    public EventSourceStatus getStatus() {
        return EventSourceStatus.builder()
                .healthy(isHealthy())
                .sourceType(getSourceType())
                .eventsConsumed(eventsConsumed.get())
                .eventsProcessed(eventsProcessed.get())
                .eventsFailed(eventsFailed.get())
                .lastEventTime(lastEventTime)
                .startTime(startTime)
                .currentState(running.get() ? "RUNNING" : "STOPPED")
                .metadata(getStatusMetadata())
                .errorMessage(getLastError())
                .build();
    }

    @Override
    public void acknowledge(EventEnvelope<?> envelope) {
        eventsProcessed.incrementAndGet();
        if (processedCounter != null) {
            processedCounter.increment();
        }
        doAcknowledge(envelope);
        log.debug("Acknowledged event: {}", envelope.getEventId());
    }

    @Override
    public void reportError(EventEnvelope<?> envelope, Throwable error) {
        eventsFailed.incrementAndGet();
        if (failedCounter != null) {
            failedCounter.increment();
        }
        doReportError(envelope, error);
        log.error("Error processing event {}: {}", envelope.getEventId(), error.getMessage(), error);
    }

    /**
     * Bridge method to convert blocking stream to reactive flux.
     */
    protected Flux<EventEnvelope<?>> bridgeToReactive(Stream<EventEnvelope<?>> stream) {
        return Flux.create(sink -> {
            Thread.ofVirtual().start(() -> {
                try {
                    stream.forEach(event -> {
                        if (!running.get()) {
                            return;
                        }
                        sink.next(event);
                        recordEventConsumed(event);
                    });
                    sink.complete();
                } catch (Exception e) {
                    sink.error(e);
                }
            });
        });
    }

    protected void recordEventConsumed(EventEnvelope<?> event) {
        eventsConsumed.incrementAndGet();
        lastEventTime = Instant.now();
        if (consumedCounter != null) {
            consumedCounter.increment();
        }
        log.trace("Consumed event: {} of type: {}", event.getEventId(), event.getEventType());
    }

    // Abstract methods to be implemented by subclasses
    protected abstract void doStart();
    protected abstract void doStop();
    protected abstract boolean doHealthCheck();
    protected abstract Map<String, Object> getStatusMetadata();
    protected abstract String getLastError();
    protected abstract void doAcknowledge(EventEnvelope<?> envelope);
    protected abstract void doReportError(EventEnvelope<?> envelope, Throwable error);
}
