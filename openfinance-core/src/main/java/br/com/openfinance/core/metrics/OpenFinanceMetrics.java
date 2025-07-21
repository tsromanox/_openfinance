package br.com.openfinance.core.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class OpenFinanceMetrics {

    private final MeterRegistry registry;

    public void recordApiCall(String institution, String endpoint, int status, long duration) {
        Timer.builder("openfinance.api.call")
                .tag("institution", institution)
                .tag("endpoint", endpoint)
                .tag("status", String.valueOf(status))
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(duration, TimeUnit.MILLISECONDS);
    }

    public void incrementProcessedAccounts(String type) {
        Counter.builder("openfinance.accounts.processed")
                .tag("type", type)
                .description("Number of accounts processed")
                .register(registry)
                .increment();
    }

    public void recordBatchProcessingTime(String operation, Duration duration) {
        Timer.builder("openfinance.batch.processing")
                .tag("operation", operation)
                .description("Batch processing time")
                .register(registry)
                .record(duration);
    }

    public void incrementErrors(String type, String error) {
        Counter.builder("openfinance.errors")
                .tag("type", type)
                .tag("error", error)
                .description("Number of errors")
                .register(registry)
                .increment();
    }
}
