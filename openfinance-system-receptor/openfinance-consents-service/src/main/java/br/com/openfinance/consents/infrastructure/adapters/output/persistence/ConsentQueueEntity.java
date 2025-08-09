package br.com.openfinance.consents.infrastructure.adapters.output.persistence;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Consent {
    private UUID consentId;
    private String customerId;
    private String participantId;
    private ConsentStatus status;
    private Set<ConsentPermission> permissions;
    private LocalDateTime creationDateTime;
    private LocalDateTime expirationDateTime;
    private LocalDateTime statusUpdateDateTime;
    private String transmitterConsentId;

    public boolean isValid() {
        return status == ConsentStatus.AUTHORISED
                && LocalDateTime.now().isBefore(expirationDateTime);
    }

    public void revoke() {
        this.status = ConsentStatus.REJECTED;
        this.statusUpdateDateTime = LocalDateTime.now();
    }
}

// ConsentQueueEntity.java - Queue Table for Processing
package br.com.openfinance.consents.infrastructure.adapters.output.persistence;

import jakarta.persistence.*;
        import lombok.*;
        import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "consent_queue",
        indexes = {
                @Index(name = "idx_status_priority", columnList = "status,priority,created_at"),
                @Index(name = "idx_participant", columnList = "participant_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentQueueEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "consent_id", nullable = false)
    private String consentId;

    @Column(name = "participant_id", nullable = false)
    private String participantId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QueueStatus status;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public enum QueueStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, RETRYING
    }
}
