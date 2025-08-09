package br.com.openfinance.core.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OAuth2ClientConfig {

    @Value("${security.oauth2.client.registration.openfinance.client-id}")
    private String clientId;

    @Value("${security.oauth2.client.registration.openfinance.client-secret}")
    private String clientSecret;

    @Value("${security.oauth2.client.provider.openfinance.token-uri}")
    private String tokenUri;

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(openFinanceClientRegistration());
    }

    private ClientRegistration openFinanceClientRegistration() {
        return ClientRegistration.withRegistrationId("openfinance")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("consents", "accounts", "resources")
                .tokenUri(tokenUri)
                .build();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);

        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    public RestTemplate oauth2RestTemplate(OAuth2AuthorizedClientManager authorizedClientManager) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new OAuth2ClientHttpRequestInterceptor(authorizedClientManager));
        return restTemplate;
    }
}
