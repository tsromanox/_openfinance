package br.com.openfinance.core.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class MetricsCollector {

    private final Counter consentProcessedCounter;
    private final Counter consentFailedCounter;
    private final Timer consentProcessingTimer;
    private final Counter accountSyncCounter;

    public MetricsCollector(MeterRegistry registry) {
        this.consentProcessedCounter = Counter.builder("consents.processed")
                .description("Number of consents processed")
                .register(registry);

        this.consentFailedCounter = Counter.builder("consents.failed")
                .description("Number of consent processing failures")
                .register(registry);

        this.consentProcessingTimer = Timer.builder("consent.processing.time")
                .description("Time taken to process consent")
                .register(registry);

        this.accountSyncCounter = Counter.builder("accounts.synced")
                .description("Number of accounts synchronized")
                .register(registry);
    }

    public void recordConsentProcessed() {
        consentProcessedCounter.increment();
    }

    public void recordConsentFailed() {
        consentFailedCounter.increment();
    }

    public Timer.Sample startConsentProcessingTimer() {
        return Timer.start();
    }

    public void stopConsentProcessingTimer(Timer.Sample sample) {
        sample.stop(consentProcessingTimer);
    }

    public void recordAccountSynced() {
        accountSyncCounter.increment();
    }
}
