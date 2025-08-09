package br.com.openfinance.core.infrastructure.security;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

import java.io.IOException;

public class OAuth2ClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public OAuth2ClientHttpRequestInterceptor(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("openfinance")
                .principal("system")
                .build();

        OAuth2AuthorizedClient authorizedClient =
                authorizedClientManager.authorize(authorizeRequest);

        if (authorizedClient != null) {
            request.getHeaders().setBearerAuth(
                    authorizedClient.getAccessToken().getTokenValue());
        }

        return execution.execute(request, body);
    }
}
