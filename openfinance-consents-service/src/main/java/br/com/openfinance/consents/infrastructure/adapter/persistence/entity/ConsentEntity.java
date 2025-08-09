package br.com.openfinance.consents.infrastructure.adapter.persistence.entity;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Container(containerName = "consents")
public class ConsentEntity {

    @Id
    private String id;

    @PartitionKey
    private String partitionKey; // clientId

    private String organisationId;
    private String status;
    private LocalDateTime creationDateTime;
    private LocalDateTime statusUpdateDateTime;
    private LocalDateTime expirationDateTime;
    private List<ConsentPermissionEntity> permissions;
    private String loggedUserId;
    private String businessEntityId;
    private Set<String> linkedAccountIds;
    private Set<String> linkedCreditCardAccountIds;
    private ConsentRejectionReasonEntity rejectionReason;
    private LocalDateTime transactionFromDateTime;
    private LocalDateTime transactionToDateTime;
    private LocalDateTime lastProcessedDateTime;
    private LocalDateTime lastModified;

    @Version
    private String _etag;

    private Integer ttl; // Time to live in seconds
}
