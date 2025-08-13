package br.com.openfinance.resources.infrastructure.adapters.output.persistence.entity;

import br.com.openfinance.resources.domain.model.ResourceType;
import br.com.openfinance.resources.domain.model.ResourceStatus;
import br.com.openfinance.core.infrastructure.persistence.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "resources",
    indexes = {
        @Index(name = "idx_resource_customer", columnList = "customerId"),
        @Index(name = "idx_resource_participant", columnList = "participantId"),
        @Index(name = "idx_resource_type", columnList = "type"),
        @Index(name = "idx_resource_external", columnList = "participantId, externalResourceId"),
        @Index(name = "idx_resource_sync", columnList = "lastSyncAt"),
        @Index(name = "idx_resource_status", columnList = "status")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceEntity extends AuditableEntity {
    
    @Id
    @GeneratedValue
    private UUID resourceId;

    @Column(nullable = false)
    private String externalResourceId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String participantId;

    @Column(nullable = false)
    private String brandId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceStatus status;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime lastSyncAt;

    @OneToOne(mappedBy = "resource", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ResourceMetadataEntity metadata;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (status == null) {
            status = ResourceStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        super.onUpdate();
    }
}