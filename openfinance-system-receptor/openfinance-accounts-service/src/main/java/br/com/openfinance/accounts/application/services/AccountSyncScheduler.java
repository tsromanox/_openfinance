package br.com.openfinance.accounts.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import br.com.openfinance.core.domain.events.ConsentAuthorizedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountSyncScheduler {

    private final AccountApplicationService accountService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "consent-authorized", groupId = "accounts-service")
    public void handleConsentAuthorized(String eventJson) {
        try {
            ConsentAuthorizedEvent event = objectMapper.readValue(
                    eventJson, ConsentAuthorizedEvent.class);

            log.info("Received consent authorized event for customer: {}",
                    event.getCustomerId());

            // Check if consent includes account permissions
            if (hasAccountPermissions(event)) {
                accountService.syncAccountsByCustomer(event.getCustomerId());
            }

        } catch (Exception e) {
            log.error("Error processing consent authorized event", e);
        }
    }

    private boolean hasAccountPermissions(ConsentAuthorizedEvent event) {
        return event.getPermissions().stream()
                .anyMatch(p -> p.startsWith("ACCOUNTS_"));
    }
}
