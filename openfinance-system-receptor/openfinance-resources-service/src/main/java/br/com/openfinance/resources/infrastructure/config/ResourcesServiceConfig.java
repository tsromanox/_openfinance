package br.com.openfinance.resources.infrastructure.config;

import br.com.openfinance.resources.domain.ports.output.ResourceEventPublisher;
import br.com.openfinance.core.infrastructure.monitoring.Monitored;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableCaching
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "br.com.openfinance.resources.infrastructure.adapters.output.persistence.repository")
@Slf4j
public class ResourcesServiceConfig {

    @Bean
    @Primary
    public ResourceEventPublisher resourceEventPublisher() {
        return new ResourceEventPublisher() {
            @Override
            @Monitored
            public void publishResourceSynced(br.com.openfinance.resources.domain.model.Resource resource) {
                log.info("Publishing resource synced event: {}", resource.getResourceId());
                // In a real implementation, this would publish to Kafka
                // For now, just log the event
            }

            @Override
            @Monitored
            public void publishResourceCreated(br.com.openfinance.resources.domain.model.Resource resource) {
                log.info("Publishing resource created event: {}", resource.getResourceId());
                // In a real implementation, this would publish to Kafka
            }

            @Override
            @Monitored
            public void publishResourceUpdated(br.com.openfinance.resources.domain.model.Resource resource) {
                log.info("Publishing resource updated event: {}", resource.getResourceId());
                // In a real implementation, this would publish to Kafka
            }

            @Override
            @Monitored
            public void publishBatchSyncCompleted(int syncedCount) {
                log.info("Publishing batch sync completed event: {} resources synced", syncedCount);
                // In a real implementation, this would publish to Kafka
            }

            @Override
            @Monitored
            public void publishSyncError(String resourceId, String error) {
                log.error("Publishing sync error event for resource {}: {}", resourceId, error);
                // In a real implementation, this would publish to Kafka
            }
        };
    }
}