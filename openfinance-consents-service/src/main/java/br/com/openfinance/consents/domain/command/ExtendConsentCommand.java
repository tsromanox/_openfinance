package br.com.openfinance.consents.domain.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtendConsentCommand {
    private String consentId;
    private LocalDateTime expirationDateTime;
    private String loggedUserId;
    private String businessEntityId;
    private String ipAddress;
    private String userAgent;
}
