package br.com.openfinance.resources.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "resource_metadata")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceMetadataEntity {
    
    @Id
    @GeneratedValue
    private UUID metadataId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private ResourceEntity resource;

    private String currency;

    @Column(precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(precision = 19, scale = 4)
    private BigDecimal availableAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal blockedAmount;

    private LocalDate dueDate;

    private LocalDate expiryDate;

    private String contractNumber;

    @ElementCollection
    @CollectionTable(
        name = "resource_metadata_additional_info",
        joinColumns = @JoinColumn(name = "metadata_id")
    )
    @MapKeyColumn(name = "info_key")
    @Column(name = "info_value")
    private Map<String, String> additionalInfo;
}