package br.com.openfinance.resources.domain.ports.output;

import br.com.openfinance.resources.domain.model.Resource;

public interface ResourceEventPublisher {
    void publishResourceSynced(Resource resource);
    void publishResourceCreated(Resource resource);
    void publishResourceUpdated(Resource resource);
    void publishBatchSyncCompleted(int syncedCount);
    void publishSyncError(String resourceId, String error);
}