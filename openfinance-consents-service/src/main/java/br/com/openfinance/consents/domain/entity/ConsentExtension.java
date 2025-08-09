package br.com.openfinance.consents.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentExtension {
    private String id;
    private String consentId;
    private LocalDateTime previousExpirationDateTime;
    private LocalDateTime newExpirationDateTime;
    private LocalDateTime requestDateTime;
    private String loggedUserId;
    private String ipAddress;
    private String userAgent;
    private ConsentStatus status;
    private List<String> permissions;
    private LocalDateTime creationDateTime;
    private LocalDateTime statusUpdateDateTime;
}
