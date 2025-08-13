package br.com.openfinance.resources.domain.ports.input;

import br.com.openfinance.resources.domain.model.Resource;
import java.util.Optional;
import java.util.UUID;

public interface GetResourceUseCase {
    Optional<Resource> getResource(UUID resourceId);
    Optional<Resource> getResourceByExternalId(String participantId, String externalResourceId);
}