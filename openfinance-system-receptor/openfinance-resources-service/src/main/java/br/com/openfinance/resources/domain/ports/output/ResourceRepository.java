package br.com.openfinance.resources.domain.ports.output;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceRepository {
    Optional<Resource> findById(UUID resourceId);
    Optional<Resource> findByExternalId(String participantId, String externalResourceId);
    Page<Resource> findByCustomerId(String customerId, Pageable pageable);
    Page<Resource> findByParticipantId(String participantId, Pageable pageable);
    Page<Resource> findByType(ResourceType type, Pageable pageable);
    Page<Resource> findByCustomerIdAndType(String customerId, ResourceType type, Pageable pageable);
    List<Resource> findResourcesForBatchUpdate(int limit);
    Page<Resource> findAllActive(Pageable pageable);
    List<Resource> findResourcesNeedingSync(int limit);
    Resource save(Resource resource);
    void saveAll(List<Resource> resources);
    void deleteById(UUID resourceId);
}