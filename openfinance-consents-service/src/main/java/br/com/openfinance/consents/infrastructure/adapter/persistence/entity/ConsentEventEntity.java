package br.com.openfinance.consents.infrastructure.adapter.persistence.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Container(containerName = "consent-events")
public class ConsentEventEntity {

    @Id
    private String id;

    @PartitionKey
    private String partitionKey; // consentId

    private String type;
    private LocalDateTime timestamp;
    private String userId;
    private String details;
    private String previousStatus;
    private String newStatus;

    private Integer ttl; // 30 days retention
}
