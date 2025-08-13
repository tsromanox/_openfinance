package br.com.openfinance.infrastructure.scheduler.config;

import br.com.openfinance.infrastructure.scheduler.service.VirtualThreadProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quartz job implementation that leverages Virtual Thread processing service.
 */
@Slf4j
@Component
public class VirtualThreadBatchProcessingJob implements Job {

    @Autowired
    private VirtualThreadProcessingService processingService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.debug("Starting batch processing job execution");
            
            // Trigger the Virtual Thread batch processing
            processingService.processPendingJobsWithVirtualThreads()
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("Batch processing job failed", throwable);
                        } else {
                            log.debug("Batch processing job completed successfully. Processed: {} jobs", result);
                        }
                    });
            
        } catch (Exception e) {
            log.error("Error executing batch processing job", e);
            throw new JobExecutionException(e);
        }
    }
}