package br.com.openfinance.accounts.infrastructure.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class VirtualThreadMetrics {

    private final MeterRegistry meterRegistry;
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Scheduled(fixedDelay = 10000) // Every 10 seconds
    public void collectVirtualThreadMetrics() {
        // Get thread counts
        int totalThreads = threadMXBean.getThreadCount();
        int daemonThreads = threadMXBean.getDaemonThreadCount();
        long totalStartedThreads = threadMXBean.getTotalStartedThreadCount();

        // Register metrics
        meterRegistry.gauge("jvm.threads.virtual.current", Tags.empty(), totalThreads);
        meterRegistry.gauge("jvm.threads.virtual.daemon", Tags.empty(), daemonThreads);
        meterRegistry.gauge("jvm.threads.virtual.total.started", Tags.empty(), totalStartedThreads);

        // Log for debugging
        log.debug("Virtual Thread Metrics - Total: {}, Daemon: {}, Started: {}",
                totalThreads, daemonThreads, totalStartedThreads);
    }

    public void recordVirtualThreadExecution(String operation, long durationMs) {
        meterRegistry.timer("virtual.thread.execution", "operation", operation)
                .record(Duration.ofMillis(durationMs));
    }
}
