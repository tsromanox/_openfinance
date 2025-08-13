package br.com.openfinance.resources.infrastructure.adapters.output.persistence;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceType;
import br.com.openfinance.resources.domain.ports.output.ResourceRepository;
import br.com.openfinance.resources.infrastructure.adapters.output.persistence.entity.ResourceEntity;
import br.com.openfinance.resources.infrastructure.adapters.output.persistence.mapper.ResourcePersistenceMapper;
import br.com.openfinance.resources.infrastructure.adapters.output.persistence.repository.ResourceJpaRepository;
import br.com.openfinance.core.infrastructure.monitoring.Monitored;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ResourceRepositoryAdapter implements ResourceRepository {

    private final ResourceJpaRepository jpaRepository;
    private final ResourcePersistenceMapper mapper;

    @Override
    @Monitored
    public Optional<Resource> findById(UUID resourceId) {
        log.debug("Finding resource by ID: {}", resourceId);
        return jpaRepository.findById(resourceId)
                .map(mapper::toDomain);
    }

    @Override
    @Monitored
    public Optional<Resource> findByExternalId(String participantId, String externalResourceId) {
        log.debug("Finding resource by external ID: {} for participant: {}", externalResourceId, participantId);
        return jpaRepository.findByParticipantIdAndExternalResourceId(participantId, externalResourceId)
                .map(mapper::toDomain);
    }

    @Override
    @Monitored
    public Page<Resource> findByCustomerId(String customerId, Pageable pageable) {
        log.debug("Finding resources by customer ID: {}", customerId);
        Page<ResourceEntity> entityPage = jpaRepository.findByCustomerId(customerId, pageable);
        List<Resource> resources = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(resources, pageable, entityPage.getTotalElements());
    }

    @Override
    @Monitored
    public Page<Resource> findByParticipantId(String participantId, Pageable pageable) {
        log.debug("Finding resources by participant ID: {}", participantId);
        Page<ResourceEntity> entityPage = jpaRepository.findByParticipantId(participantId, pageable);
        List<Resource> resources = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(resources, pageable, entityPage.getTotalElements());
    }

    @Override
    @Monitored
    public Page<Resource> findByType(ResourceType type, Pageable pageable) {
        log.debug("Finding resources by type: {}", type);
        Page<ResourceEntity> entityPage = jpaRepository.findByType(type, pageable);
        List<Resource> resources = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(resources, pageable, entityPage.getTotalElements());
    }

    @Override
    @Monitored
    public Page<Resource> findByCustomerIdAndType(String customerId, ResourceType type, Pageable pageable) {
        log.debug("Finding resources by customer ID: {} and type: {}", customerId, type);
        Page<ResourceEntity> entityPage = jpaRepository.findByCustomerIdAndType(customerId, type, pageable);
        List<Resource> resources = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(resources, pageable, entityPage.getTotalElements());
    }

    @Override
    @Monitored
    public List<Resource> findResourcesForBatchUpdate(int limit) {
        log.debug("Finding resources for batch update, limit: {}", limit);
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
        Pageable pageable = PageRequest.of(0, limit);
        
        List<ResourceEntity> entities = jpaRepository.findResourcesForBatchUpdate(cutoffTime, pageable);
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Monitored
    public Page<Resource> findAllActive(Pageable pageable) {
        log.debug("Finding all active resources");
        Page<ResourceEntity> entityPage = jpaRepository.findByStatus(
                br.com.openfinance.resources.domain.model.ResourceStatus.ACTIVE, 
                pageable
        );
        List<Resource> resources = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        return new PageImpl<>(resources, pageable, entityPage.getTotalElements());
    }

    @Override
    @Monitored
    public List<Resource> findResourcesNeedingSync(int limit) {
        log.debug("Finding resources needing sync, limit: {}", limit);
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
        Pageable pageable = PageRequest.of(0, limit);
        
        List<ResourceEntity> entities = jpaRepository.findResourcesNeedingSync(cutoffTime, pageable);
        
        return entities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Monitored
    public Resource save(Resource resource) {
        log.debug("Saving resource: {}", resource.getResourceId());
        
        ResourceEntity entity;
        if (resource.getResourceId() != null) {
            // Update existing
            entity = jpaRepository.findById(resource.getResourceId())
                    .orElse(mapper.toEntity(resource));
            mapper.updateEntityFromDomain(resource, entity);
        } else {
            // Create new
            entity = mapper.toEntity(resource);
        }
        
        ResourceEntity savedEntity = jpaRepository.save(entity);
        Resource savedResource = mapper.toDomain(savedEntity);
        
        log.debug("Successfully saved resource: {}", savedResource.getResourceId());
        return savedResource;
    }

    @Override
    @Monitored
    public void saveAll(List<Resource> resources) {
        log.debug("Saving {} resources", resources.size());
        
        List<ResourceEntity> entities = resources.stream()
                .map(resource -> {
                    if (resource.getResourceId() != null) {
                        ResourceEntity existing = jpaRepository.findById(resource.getResourceId())
                                .orElse(mapper.toEntity(resource));
                        mapper.updateEntityFromDomain(resource, existing);
                        return existing;
                    } else {
                        return mapper.toEntity(resource);
                    }
                })
                .collect(Collectors.toList());
        
        jpaRepository.saveAll(entities);
        log.debug("Successfully saved {} resources", resources.size());
    }

    @Override
    @Monitored
    public void deleteById(UUID resourceId) {
        log.debug("Deleting resource by ID: {}", resourceId);
        jpaRepository.deleteById(resourceId);
        log.debug("Successfully deleted resource: {}", resourceId);
    }
}