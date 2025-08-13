package br.com.openfinance.resources.application.dto;

import br.com.openfinance.resources.domain.model.ResourceType;
import br.com.openfinance.resources.domain.model.ResourceStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDTO {
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
    private ResourceMetadataDTO metadata;
}