package br.com.openfinance.config;

import br.com.openfinance.application.port.output.ResourceDiscoveryService;
import br.com.openfinance.application.port.output.ResourceRepository;
import br.com.openfinance.application.service.ResourceService;
import br.com.openfinance.service.resources.VirtualThreadResourceService;
import br.com.openfinance.service.resources.config.VirtualThreadResourceConfig;
import br.com.openfinance.service.resources.monitoring.ResourcePerformanceMonitor;
import br.com.openfinance.service.resources.processor.VirtualThreadResourceProcessor;
import br.com.openfinance.service.resources.resource.AdaptiveResourceResourceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Resource service configuration for OpenFinance Receptor.
 * Configures all components needed for high-performance resource processing
 * with Virtual Threads and adaptive resource management.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties
@ConditionalOnProperty(value = "openfinance.resources.enabled", havingValue = "true", matchIfMissing = true)
@Import({
    VirtualThreadResourceConfig.class
})
public class ResourceConfiguration {
    
    /**
     * Configure the main resource service that orchestrates all resource operations.
     */
    @Bean
    public ResourceService resourceService(
            ResourceRepository resourceRepository,
            ResourceDiscoveryService resourceDiscoveryService,
            VirtualThreadResourceService virtualThreadResourceService) {
        
        log.info("Configuring ResourceService with Virtual Thread support");
        
        return new ResourceService(
                resourceRepository,
                resourceDiscoveryService,
                virtualThreadResourceService
        );
    }
    
    /**
     * Configure resource discovery service implementation.
     */
    @Bean
    @ConditionalOnProperty(value = "openfinance.resources.discovery.enabled", havingValue = "true", matchIfMissing = true)
    public ResourceDiscoveryService resourceDiscoveryService() {
        log.info("Configuring ResourceDiscoveryService");
        
        return new ResourceDiscoveryService() {
            @Override
            public java.util.concurrent.CompletableFuture<java.util.List<br.com.openfinance.service.resources.domain.Resource>> 
                    discoverFromEndpoint(String discoveryEndpoint) {
                
                return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    // Implementation would connect to Open Finance Brasil directory
                    log.debug("Discovering resources from endpoint: {}", discoveryEndpoint);
                    
                    // Simulate resource discovery - in real implementation would call external API
                    try {
                        Thread.sleep(100); // Simulate network call
                        
                        return java.util.List.of(
                            br.com.openfinance.service.resources.domain.Resource.builder()
                                .resourceId("discovered-" + discoveryEndpoint.hashCode())
                                .organizationId("org-" + discoveryEndpoint.hashCode())
                                .organizationName("Organization " + discoveryEndpoint.hashCode())
                                .type(br.com.openfinance.service.resources.domain.ResourceType.BANK)
                                .status(br.com.openfinance.service.resources.domain.ResourceStatus.DISCOVERED)
                                .discoveredAt(java.time.LocalDateTime.now())
                                .createdAt(java.time.LocalDateTime.now())
                                .updatedAt(java.time.LocalDateTime.now())
                                .build()
                        );
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Discovery interrupted", e);
                    }
                });
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<java.util.List<br.com.openfinance.service.resources.domain.Resource>> 
                    discoverFromEndpoints(java.util.List<String> discoveryEndpoints) {
                
                return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    return discoveryEndpoints.stream()
                            .map(this::discoverFromEndpoint)
                            .map(future -> {
                                try {
                                    return future.get();
                                } catch (Exception e) {
                                    log.error("Failed to discover from endpoint: {}", e.getMessage());
                                    return java.util.List.<br.com.openfinance.service.resources.domain.Resource>of();
                                }
                            })
                            .flatMap(java.util.Collection::stream)
                            .toList();
                });
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<Boolean> validateDiscoveryEndpoint(String endpoint) {
                return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    // Validate endpoint availability
                    return endpoint != null && endpoint.startsWith("http");
                });
            }
            
            @Override
            public java.util.List<String> getKnownDiscoveryEndpoints() {
                return java.util.List.of(
                    "https://data.directory.openbankingbrasil.org.br/participants",
                    "https://opendata.providers.org.br/discovery"
                );
            }
            
            @Override
            public java.util.concurrent.CompletableFuture<java.util.List<String>> refreshDiscoveryEndpoints() {
                return java.util.concurrent.CompletableFuture.completedFuture(getKnownDiscoveryEndpoints());
            }
        };
    }
    
    /**
     * Configure resource repository implementation.
     */
    @Bean
    public ResourceRepository resourceRepository() {
        log.info("Configuring ResourceRepository");
        
        return new ResourceRepository() {
            private final java.util.concurrent.ConcurrentHashMap<String, br.com.openfinance.service.resources.domain.Resource> 
                    resources = new java.util.concurrent.ConcurrentHashMap<>();
            
            @Override
            public br.com.openfinance.service.resources.domain.Resource save(br.com.openfinance.service.resources.domain.Resource resource) {
                resources.put(resource.getResourceId(), resource);
                log.debug("Saved resource: {}", resource.getResourceId());
                return resource;
            }
            
            @Override
            public java.util.List<br.com.openfinance.service.resources.domain.Resource> saveAll(
                    java.util.List<br.com.openfinance.service.resources.domain.Resource> resourceList) {
                resourceList.forEach(this::save);
                log.debug("Saved {} resources", resourceList.size());
                return resourceList;
            }
            
            @Override
            public java.util.Optional<br.com.openfinance.service.resources.domain.Resource> findById(String resourceId) {
                return java.util.Optional.ofNullable(resources.get(resourceId));
            }
            
            @Override
            public java.util.List<br.com.openfinance.service.resources.domain.Resource> findByOrganizationId(String organizationId) {
                return resources.values().stream()
                        .filter(r -> organizationId.equals(r.getOrganizationId()))
                        .toList();
            }
            
            @Override
            public java.util.List<br.com.openfinance.service.resources.domain.Resource> findByTypeAndStatus(
                    br.com.openfinance.service.resources.domain.ResourceType type, 
                    br.com.openfinance.service.resources.domain.ResourceStatus status) {
                return resources.values().stream()
                        .filter(r -> type.equals(r.getType()) && status.equals(r.getStatus()))
                        .toList();
            }
            
            @Override
            public java.util.List<br.com.openfinance.service.resources.domain.Resource> findByStatus(
                    br.com.openfinance.service.resources.domain.ResourceStatus status) {
                return resources.values().stream()
                        .filter(r -> status.equals(r.getStatus()))
                        .toList();
            }
            
            @Override
            public java.util.List<br.com.openfinance.service.resources.domain.Resource> findByType(
                    br.com.openfinance.service.resources.domain.ResourceType type) {
                return resources.values().stream()
                        .filter(r -> type.equals(r.getType()))
                        .toList();
            }
            
            @Override
            public java.util.List<br.com.openfinance.service.resources.domain.Resource> findAll() {
                return java.util.List.copyOf(resources.values());
            }
            
            @Override
            public java.util.List<br.com.openfinance.service.resources.domain.Resource> findResourcesNeedingSync(
                    java.time.LocalDateTime threshold) {
                return resources.values().stream()
                        .filter(r -> r.getLastSyncedAt() == null || r.getLastSyncedAt().isBefore(threshold))
                        .toList();
            }
            
            @Override
            public java.util.List<br.com.openfinance.service.resources.domain.Resource> findResourcesNeedingValidation(
                    java.time.LocalDateTime threshold) {
                return resources.values().stream()
                        .filter(r -> r.getLastValidatedAt() == null || r.getLastValidatedAt().isBefore(threshold))
                        .toList();
            }
            
            @Override
            public java.util.List<br.com.openfinance.service.resources.domain.Resource> findResourcesNeedingMonitoring(
                    java.time.LocalDateTime threshold) {
                return resources.values().stream()
                        .filter(r -> r.getLastMonitoredAt() == null || r.getLastMonitoredAt().isBefore(threshold))
                        .toList();
            }
            
            @Override
            public java.util.List<br.com.openfinance.service.resources.domain.Resource> findResourcesDiscoveredAfter(
                    java.time.LocalDateTime date) {
                return resources.values().stream()
                        .filter(r -> r.getDiscoveredAt().isAfter(date))
                        .toList();
            }
            
            @Override
            public java.util.List<br.com.openfinance.service.resources.domain.Resource> findByOrganizationCnpj(String cnpj) {
                return resources.values().stream()
                        .filter(r -> cnpj.equals(r.getCnpj()))
                        .toList();
            }
            
            @Override
            public long countByStatus(br.com.openfinance.service.resources.domain.ResourceStatus status) {
                return resources.values().stream()
                        .filter(r -> status.equals(r.getStatus()))
                        .count();
            }
            
            @Override
            public long countByType(br.com.openfinance.service.resources.domain.ResourceType type) {
                return resources.values().stream()
                        .filter(r -> type.equals(r.getType()))
                        .count();
            }
            
            @Override
            public void deleteById(String resourceId) {
                resources.remove(resourceId);
                log.debug("Deleted resource: {}", resourceId);
            }
            
            @Override
            public boolean existsById(String resourceId) {
                return resources.containsKey(resourceId);
            }
            
            @Override
            public void updateStatus(String resourceId, br.com.openfinance.service.resources.domain.ResourceStatus status) {
                var resource = resources.get(resourceId);
                if (resource != null) {
                    var updated = resource.withStatus(status);
                    resources.put(resourceId, updated);
                    log.debug("Updated resource {} status to {}", resourceId, status);
                }
            }
            
            @Override
            public void updateLastSyncAt(String resourceId, java.time.LocalDateTime lastSyncAt) {
                var resource = resources.get(resourceId);
                if (resource != null) {
                    var updated = resource.withLastSyncedAt(lastSyncAt);
                    resources.put(resourceId, updated);
                    log.debug("Updated resource {} last sync time", resourceId);
                }
            }
            
            @Override
            public void updateLastValidatedAt(String resourceId, java.time.LocalDateTime lastValidatedAt) {
                var resource = resources.get(resourceId);
                if (resource != null) {
                    var updated = resource.withLastValidatedAt(lastValidatedAt);
                    resources.put(resourceId, updated);
                    log.debug("Updated resource {} last validation time", resourceId);
                }
            }
            
            @Override
            public void updateLastMonitoredAt(String resourceId, java.time.LocalDateTime lastMonitoredAt) {
                var resource = resources.get(resourceId);
                if (resource != null) {
                    var updated = resource.withLastMonitoredAt(lastMonitoredAt);
                    resources.put(resourceId, updated);
                    log.debug("Updated resource {} last monitoring time", resourceId);
                }
            }
        };
    }
}