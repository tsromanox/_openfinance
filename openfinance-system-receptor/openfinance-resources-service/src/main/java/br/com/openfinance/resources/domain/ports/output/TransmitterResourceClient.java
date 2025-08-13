package br.com.openfinance.resources.domain.ports.output;

import br.com.openfinance.resources.domain.model.Resource;
import br.com.openfinance.resources.domain.model.ResourceType;
import java.util.List;

public interface TransmitterResourceClient {
    Resource fetchResource(String participantId, String externalResourceId);
    List<Resource> fetchResourcesByCustomer(String participantId, String customerId);
    List<Resource> fetchResourcesByType(String participantId, ResourceType type);
    Resource fetchResourceDetails(String participantId, String externalResourceId);
}