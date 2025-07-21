package br.com.openfinance.accounts.application.scheduler;

import br.com.openfinance.accounts.application.service.AccountUpdateOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AccountUpdateScheduler {

    @Bean
    public JobDetail accountUpdateJobDetail() {
        return JobBuilder.newJob(AccountUpdateJob.class)
                .withIdentity("accountUpdateJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger accountUpdateTrigger(JobDetail accountUpdateJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(accountUpdateJobDetail)
                .withIdentity("accountUpdateTrigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(12)
                        .repeatForever())
                .build();
    }

    @Slf4j
    @RequiredArgsConstructor
    public static class AccountUpdateJob implements Job {

        private final AccountUpdateOrchestrator orchestrator;

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            log.info("Starting scheduled account update job");

            try {
                orchestrator.orchestrateAccountUpdates()
                        .doOnSuccess(result -> log.info("Account update job completed. Processed: {}", result))
                        .doOnError(error -> log.error("Account update job failed", error))
                        .subscribe();
            } catch (Exception e) {
                throw new JobExecutionException("Failed to execute account update job", e);
            }
        }
    }
}
