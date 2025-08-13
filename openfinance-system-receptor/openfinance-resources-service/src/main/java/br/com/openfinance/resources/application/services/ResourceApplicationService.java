package br.com.openfinance.resources.application.services;

import br.com.openfinance.resources.domain.model.*;
import br.com.openfinance.resources.domain.ports.input.*;
import br.com.openfinance.resources.domain.ports.output.*;
import br.com.openfinance.resources.domain.services.ResourceDomainService;
import br.com.openfinance.core.domain.exceptions.BusinessException;
import br.com.openfinance.core.domain.exceptions.ResourceNotFoundException;
import br.com.openfinance.core.infrastructure.monitoring.Monitored;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ResourceApplicationService implements
        GetResourceUseCase,
        ListResourcesUseCase,
        SyncResourceUseCase {

    private final ResourceRepository resourceRepository;
    private final TransmitterResourceClient transmitterClient;
    private final ResourceEventPublisher eventPublisher;
    private final ResourceDomainService domainService;

    @Override
    @Monitored
    @Cacheable(value = "resources", key = "#resourceId")
    public Optional<Resource> getResource(UUID resourceId) {
        log.debug("Getting resource: {}", resourceId);
        return resourceRepository.findById(resourceId);
    }

    @Override
    @Monitored
    public Optional<Resource> getResourceByExternalId(String participantId, String externalResourceId) {
        log.debug("Getting resource by external ID: {} from participant: {}",
                externalResourceId, participantId);
        return resourceRepository.findByExternalId(participantId, externalResourceId);
    }

    @Override
    @Monitored
    public Page<Resource> listResourcesByCustomer(String customerId, Pageable pageable) {
        log.debug("Listing resources for customer: {}", customerId);
        return resourceRepository.findByCustomerId(customerId, pageable);
    }

    @Override
    @Monitored
    public Page<Resource> listResourcesByParticipant(String participantId, Pageable pageable) {
        log.debug("Listing resources for participant: {}", participantId);
        return resourceRepository.findByParticipantId(participantId, pageable);
    }

    @Override
    @Monitored
    public Page<Resource> listResourcesByType(ResourceType type, Pageable pageable) {
        log.debug("Listing resources by type: {}", type);
        return resourceRepository.findByType(type, pageable);
    }

    @Override
    @Monitored
    public Page<Resource> listResourcesByCustomerAndType(String customerId, ResourceType type, Pageable pageable) {
        log.debug("Listing resources for customer: {} and type: {}", customerId, type);
        return resourceRepository.findByCustomerIdAndType(customerId, type, pageable);
    }

    @Override
    @Monitored
    @CacheEvict(value = "resources", key = "#resourceId")
    public Resource syncResource(UUID resourceId) {
        log.info("Syncing resource: {}", resourceId);

        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId.toString()));

        if (!domainService.shouldSyncResource(resource)) {
            log.info("Resource {} was recently synced, skipping", resourceId);
            return resource;
        }

        try {
            // Validate resource before sync
            domainService.validateResourceStatus(resource);

            // Fetch updated data from transmitter
            Resource externalData = transmitterClient.fetchResource(
                    resource.getParticipantId(),
                    resource.getExternalResourceId()
            );

            // Update resource with external data
            domainService.updateResourceFromExternal(resource, externalData);

            // Mark as synced
            domainService.markResourceAsSynced(resource);

            // Save updated resource
            resource = resourceRepository.save(resource);

            // Publish event
            eventPublisher.publishResourceSynced(resource);

            log.info("Successfully synced resource: {}", resourceId);
            return resource;

        } catch (Exception e) {
            log.error("Error syncing resource {}: {}", resourceId, e.getMessage());
            eventPublisher.publishSyncError(resourceId.toString(), e.getMessage());
            throw new BusinessException("Failed to sync resource", "SYNC_ERROR", e);
        }
    }

    @Override
    @Monitored
    public void syncResourcesByCustomer(String customerId) {
        log.info("Syncing all resources for customer: {}", customerId);

        Page<Resource> resources = resourceRepository.findByCustomerId(customerId, Pageable.unpaged());

        resources.forEach(resource -> {
            try {
                syncResource(resource.getResourceId());
            } catch (Exception e) {
                log.error("Error syncing resource {} for customer {}: {}",
                        resource.getResourceId(), customerId, e.getMessage());
            }
        });

        log.info("Completed syncing {} resources for customer {}",
                resources.getTotalElements(), customerId);
    }

    @Override
    @Monitored
    public void syncAllResources() {
        log.info("Starting sync for all resources");

        List<Resource> resources = resourceRepository.findResourcesForBatchUpdate(1000);
        int syncedCount = 0;
        int errorCount = 0;

        for (Resource resource : resources) {
            try {
                syncResource(resource.getResourceId());
                syncedCount++;
            } catch (Exception e) {
                log.error("Error syncing resource {}: {}",
                        resource.getResourceId(), e.getMessage());
                errorCount++;
            }
        }

        eventPublisher.publishBatchSyncCompleted(syncedCount);
        log.info("Batch sync completed. Synced: {}, Errors: {}", syncedCount, errorCount);
    }

    @Monitored
    @Transactional
    public Resource createResource(Resource resource) {
        log.info("Creating new resource for customer: {}", resource.getCustomerId());

        try {
            // Validate resource
            domainService.validateResource(resource);

            // Save resource
            Resource savedResource = resourceRepository.save(resource);

            // Publish event
            eventPublisher.publishResourceCreated(savedResource);

            log.info("Successfully created resource: {}", savedResource.getResourceId());
            return savedResource;

        } catch (Exception e) {
            log.error("Error creating resource for customer {}: {}", resource.getCustomerId(), e.getMessage());
            throw new BusinessException("Failed to create resource", "CREATION_ERROR", e);
        }
    }

    @Monitored
    @Transactional
    @CacheEvict(value = "resources", key = "#resource.resourceId")
    public Resource updateResource(Resource resource) {
        log.info("Updating resource: {}", resource.getResourceId());

        try {
            // Validate resource exists
            Resource existingResource = resourceRepository.findById(resource.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resource", resource.getResourceId().toString()));

            // Validate update
            if (!domainService.canUpdateResource(existingResource, resource)) {
                throw new BusinessException("Cannot update resource - external data is newer", "OUTDATED_UPDATE");
            }

            // Save updated resource
            Resource savedResource = resourceRepository.save(resource);

            // Publish event
            eventPublisher.publishResourceUpdated(savedResource);

            log.info("Successfully updated resource: {}", resource.getResourceId());
            return savedResource;

        } catch (Exception e) {
            log.error("Error updating resource {}: {}", resource.getResourceId(), e.getMessage());
            throw new BusinessException("Failed to update resource", "UPDATE_ERROR", e);
        }
    }
}