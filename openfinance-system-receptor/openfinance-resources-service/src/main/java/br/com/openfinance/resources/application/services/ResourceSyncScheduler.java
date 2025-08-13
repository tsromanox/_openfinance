package br.com.openfinance.resources.application.services;

import br.com.openfinance.core.infrastructure.monitoring.Monitored;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "openfinance.resources.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ResourceSyncScheduler {

    private final VirtualThreadResourceBatchProcessor virtualThreadProcessor;

    @Scheduled(cron = "${openfinance.resources.batch.schedule.morning:0 0 6 * * *}")
    @Monitored
    public void executeMorningBatchSync() {
        log.info("Executing morning batch sync for resources");
        
        try {
            virtualThreadProcessor.processResourceUpdatesWithVirtualThreads();
            log.info("Morning batch sync completed successfully");
        } catch (Exception e) {
            log.error("Morning batch sync failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${openfinance.resources.batch.schedule.evening:0 0 18 * * *}")
    @Monitored
    public void executeEveningBatchSync() {
        log.info("Executing evening batch sync for resources");
        
        try {
            virtualThreadProcessor.processResourceUpdatesWithVirtualThreads();
            log.info("Evening batch sync completed successfully");
        } catch (Exception e) {
            log.error("Evening batch sync failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedDelayString = "${openfinance.resources.sync.interval:900000}")
    @Monitored
    public void executeIncrementalSync() {
        log.debug("Executing incremental sync for resources");
        
        try {
            virtualThreadProcessor.processResourcesWithPagination();
            log.debug("Incremental sync completed successfully");
        } catch (Exception e) {
            log.warn("Incremental sync failed: {}", e.getMessage());
        }
    }
}