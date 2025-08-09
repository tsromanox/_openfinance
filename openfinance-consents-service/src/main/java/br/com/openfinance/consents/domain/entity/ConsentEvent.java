package br.com.openfinance.consents.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentEvent {
    private String eventId;
    private String consentId;
    private ConsentEventType type;
    private LocalDateTime timestamp;
    private String userId;
    private String details;
    private ConsentStatus previousStatus;
    private ConsentStatus newStatus;
}
