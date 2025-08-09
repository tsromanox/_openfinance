package br.com.openfinance.consents.infrastructure.adapters.output.http;

import br.com.openfinance.core.infrastructure.http.RetryableHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransmitterHttpClient implements TransmitterClient {

    private final RetryableHttpClient httpClient;
    private final OAuth2TokenProvider tokenProvider;
    private final ParticipantRegistry participantRegistry;

    @Override
    public ConsentData fetchConsent(String participantId, String consentId) {
        log.info("Fetching consent {} from participant {}", consentId, participantId);

        // Get participant URL from registry
        String baseUrl = participantRegistry.getParticipantUrl(participantId);
        String url = String.format("%s/open-banking/consents/v3/consents/%s",
                baseUrl, consentId);

        // Get OAuth2 token
        String accessToken = tokenProvider.getAccessToken(participantId);

        // Prepare headers with FAPI requirements
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("x-fapi-interaction-id", UUID.randomUUID().toString());
        headers.set("x-fapi-auth-date", LocalDateTime.now().toString());
        headers.set("x-fapi-customer-ip-address", "10.1.1.1");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<ConsentData> response = httpClient.get(
                url, headers, ConsentData.class);

        return response.getBody();
    }

    @Override
    public AccountListData fetchAccounts(String participantId, String customerId) {
        String baseUrl = participantRegistry.getParticipantUrl(participantId);
        String url = String.format("%s/open-banking/accounts/v3/accounts", baseUrl);

        String accessToken = tokenProvider.getAccessToken(participantId);

        HttpHeaders headers = createFAPIHeaders(accessToken);
        headers.set("x-customer-id", customerId);

        ResponseEntity<AccountListData> response = httpClient.get(
                url, headers, AccountListData.class);

        return response.getBody();
    }

    private HttpHeaders createFAPIHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("x-fapi-interaction-id", UUID.randomUUID().toString());
        headers.set("x-fapi-auth-date", LocalDateTime.now().toString());
        headers.set("x-fapi-customer-ip-address", "10.1.1.1");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
