package br.com.openfinance.resources.domain.model;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    private UUID resourceId;
    private String externalResourceId;
    private String customerId;
    private String participantId;
    private String brandId;
    private ResourceType type;
    private ResourceStatus status;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastSyncAt;
    private ResourceMetadata metadata;

    public boolean isActive() {
        return status == ResourceStatus.ACTIVE;
    }

    public boolean shouldSync() {
        if (lastSyncAt == null) {
            return true;
        }
        return lastSyncAt.isBefore(LocalDateTime.now().minusMinutes(15));
    }

    public void markSynced() {
        this.lastSyncAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFromExternal(Resource externalResource) {
        this.name = externalResource.getName();
        this.description = externalResource.getDescription();
        this.status = externalResource.getStatus();
        this.metadata = externalResource.getMetadata();
        this.updatedAt = LocalDateTime.now();
    }
}