package br.com.openfinance.resources.domain.ports.input;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ListResourcesUseCase {
    Page<Resource> listResourcesByCustomer(String customerId, Pageable pageable);
    Page<Resource> listResourcesByParticipant(String participantId, Pageable pageable);
    Page<Resource> listResourcesByType(ResourceType type, Pageable pageable);
    Page<Resource> listResourcesByCustomerAndType(String customerId, ResourceType type, Pageable pageable);
}