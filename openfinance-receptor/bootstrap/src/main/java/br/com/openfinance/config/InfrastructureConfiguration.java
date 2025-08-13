package br.com.openfinance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Infrastructure layer configuration for OpenFinance Receptor.
 * Configures JPA repositories, entity scanning, and infrastructure components.
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableJpaRepositories(basePackages = {
    "br.com.openfinance.infrastructure.persistence.repository"
})
@EntityScan(basePackages = {
    "br.com.openfinance.infrastructure.persistence.entity"
})
@ComponentScan(basePackages = {
    "br.com.openfinance.infrastructure.persistence",
    "br.com.openfinance.infrastructure.client",
    "br.com.openfinance.infrastructure.scheduler"
})
public class InfrastructureConfiguration {
    
    public InfrastructureConfiguration() {
        log.info("Configuring Infrastructure layer components");
        log.info("  - JPA Repositories: br.com.openfinance.infrastructure.persistence.repository");
        log.info("  - Entity Scanning: br.com.openfinance.infrastructure.persistence.entity");
        log.info("  - Infrastructure Components: persistence, client, scheduler");
    }
}