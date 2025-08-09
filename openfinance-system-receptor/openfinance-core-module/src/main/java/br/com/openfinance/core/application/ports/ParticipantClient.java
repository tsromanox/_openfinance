package br.com.openfinance.core.application.ports;

import java.util.Optional;

public interface ParticipantClient {
    ParticipantInfo getParticipantInfo(String participantId);

    String getParticipantUrl(String participantId);

    boolean isParticipantActive(String participantId);

    record ParticipantInfo(
            String participantId,
            String organizationId,
            String organizationName,
            String status,
            String apiBaseUrl,
            String authorizationServerUrl
    ) {
    }
}