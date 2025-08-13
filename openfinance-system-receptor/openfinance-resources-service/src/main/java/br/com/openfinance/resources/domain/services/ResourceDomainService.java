package br.com.openfinance.resources.domain.services;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceStatus;
import br.com.openfinance.core.domain.exceptions.BusinessException;
import br.com.openfinance.core.infrastructure.monitoring.Monitored;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@Slf4j
public class ResourceDomainService {

    @Monitored
    public boolean shouldSyncResource(Resource resource) {
        if (resource == null) {
            return false;
        }
        
        if (!resource.isActive()) {
            log.debug("Resource {} is not active, skipping sync", resource.getResourceId());
            return false;
        }
        
        return resource.shouldSync();
    }

    @Monitored
    public void updateResourceFromExternal(Resource resource, Resource externalResource) {
        if (resource == null || externalResource == null) {
            throw new BusinessException("Resource cannot be null", "INVALID_RESOURCE");
        }

        log.debug("Updating resource {} with external data", resource.getResourceId());
        
        resource.updateFromExternal(externalResource);
        
        log.info("Resource {} updated successfully", resource.getResourceId());
    }

    @Monitored
    public void validateResource(Resource resource) {
        if (resource == null) {
            throw new BusinessException("Resource cannot be null", "INVALID_RESOURCE");
        }

        if (resource.getCustomerId() == null || resource.getCustomerId().trim().isEmpty()) {
            throw new BusinessException("Customer ID is required", "MISSING_CUSTOMER_ID");
        }

        if (resource.getParticipantId() == null || resource.getParticipantId().trim().isEmpty()) {
            throw new BusinessException("Participant ID is required", "MISSING_PARTICIPANT_ID");
        }

        if (resource.getType() == null) {
            throw new BusinessException("Resource type is required", "MISSING_RESOURCE_TYPE");
        }

        if (resource.getExternalResourceId() == null || resource.getExternalResourceId().trim().isEmpty()) {
            throw new BusinessException("External resource ID is required", "MISSING_EXTERNAL_ID");
        }
    }

    @Monitored
    public boolean isResourceExpired(Resource resource) {
        if (resource == null || resource.getMetadata() == null) {
            return false;
        }
        
        return resource.getMetadata().isExpired();
    }

    @Monitored
    public void markResourceAsSynced(Resource resource) {
        if (resource == null) {
            throw new BusinessException("Resource cannot be null", "INVALID_RESOURCE");
        }
        
        resource.markSynced();
        log.debug("Resource {} marked as synced at {}", resource.getResourceId(), LocalDateTime.now());
    }

    @Monitored
    public boolean canUpdateResource(Resource existing, Resource updated) {
        if (existing == null || updated == null) {
            return false;
        }

        // Can't update if external resource is newer
        if (existing.getUpdatedAt() != null && updated.getUpdatedAt() != null) {
            return !existing.getUpdatedAt().isAfter(updated.getUpdatedAt());
        }

        return true;
    }

    @Monitored
    public void validateResourceStatus(Resource resource) {
        if (resource == null) {
            throw new BusinessException("Resource cannot be null", "INVALID_RESOURCE");
        }

        if (resource.getStatus() == ResourceStatus.CANCELLED) {
            throw new BusinessException("Cannot operate on cancelled resource", "CANCELLED_RESOURCE");
        }

        if (isResourceExpired(resource)) {
            throw new BusinessException("Resource has expired", "EXPIRED_RESOURCE");
        }
    }
}