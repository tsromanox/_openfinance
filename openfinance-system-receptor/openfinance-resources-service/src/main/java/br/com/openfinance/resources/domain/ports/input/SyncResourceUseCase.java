package br.com.openfinance.resources.domain.ports.input;

import br.com.openfinance.resources.domain.model.Resource;
import java.util.UUID;

public interface SyncResourceUseCase {
    Resource syncResource(UUID resourceId);
    void syncResourcesByCustomer(String customerId);
    void syncAllResources();
}