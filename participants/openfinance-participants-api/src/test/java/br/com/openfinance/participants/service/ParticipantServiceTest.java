package br.com.openfinance.participants.service;

import br.com.openfinance.participants.dto.ApiDiscoveryEndpointDto;
import br.com.openfinance.participants.dto.ApiResourceDto;
import br.com.openfinance.participants.dto.AuthorisationServerDto;
import br.com.openfinance.participants.dto.ParticipantDto;
import br.com.openfinance.participants.dto.response.ApiEndpointResponseDto;
import br.com.openfinance.participants.dto.response.ParticipantResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private ParticipantService participantService;

    private ParticipantDto mockParticipant;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(participantService, "participantsApiUrl",
                "https://test.api.com/participants");
        ReflectionTestUtils.setField(participantService, "timeoutSeconds", 30);

        // Mock participant data
        mockParticipant = createMockParticipant();
    }

    @Test
    void shouldUpdateParticipantsCacheSuccessfully() {
        // Given
        List<ParticipantDto> mockParticipants = List.of(mockParticipant);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ParticipantDto.class))
                .thenReturn(Flux.fromIterable(mockParticipants));

        // When
        participantService.updateParticipantsCache();

        // Then
        Optional<ParticipantResponseDto> result =
                participantService.getParticipantByCnpj("12345678000195");

        assertThat(result).isPresent();
        assertThat(result.get().getCnpj()).isEqualTo("12345678000195");
        assertThat(result.get().getOrganisationName()).isEqualTo("Test Bank");
    }

    @Test
    void shouldReturnEmptyWhenParticipantNotFound() {
        // When
        Optional<ParticipantResponseDto> result =
                participantService.getParticipantByCnpj("99999999999999");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForInvalidCnpj() {
        // When
        Optional<ParticipantResponseDto> result =
                participantService.getParticipantByCnpj("123");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldGetApiEndpointsByCnpj() {
        // Given
        List<ParticipantDto> mockParticipants = List.of(mockParticipant);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ParticipantDto.class))
                .thenReturn(Flux.fromIterable(mockParticipants));

        participantService.updateParticipantsCache();

        // When
        List<ApiEndpointResponseDto> endpoints =
                participantService.getApiEndpointsByCnpj("12345678000195", "accounts");

        // Then
        assertThat(endpoints).isNotEmpty();
        assertThat(endpoints.get(0).getApiFamily()).isEqualTo("accounts");
        assertThat(endpoints.get(0).getBaseUrl()).isEqualTo("https://api.testbank.com.br");
    }

    @Test
    void shouldGetAvailableApiFamilies() {
        // Given
        List<ParticipantDto> mockParticipants = List.of(mockParticipant);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(ParticipantDto.class))
                .thenReturn(Flux.fromIterable(mockParticipants));

        participantService.updateParticipantsCache();

        // When
        Set<String> families = participantService.getAvailableApiFamilies();

        // Then
        assertThat(families).contains("accounts", "payments");
    }

    private ParticipantDto createMockParticipant() {
        ApiDiscoveryEndpointDto endpoint1 = ApiDiscoveryEndpointDto.builder()
                .apiDiscoveryId("endpoint1")
                .apiEndpoint("https://api.testbank.com.br/open-banking/accounts/v2")
                .build();

        ApiDiscoveryEndpointDto endpoint2 = ApiDiscoveryEndpointDto.builder()
                .apiDiscoveryId("endpoint2")
                .apiEndpoint("https://api.testbank.com.br/open-banking/payments/v3")
                .build();

        ApiResourceDto apiResource1 = ApiResourceDto.builder()
                .apiResourceId("resource1")
                .apiFamilyType("accounts")
                .apiVersion("2.0.0")
                .certificationStatus("Certified")
                .apiDiscoveryEndpoints(List.of(endpoint1))
                .build();

        ApiResourceDto apiResource2 = ApiResourceDto.builder()
                .apiResourceId("resource2")
                .apiFamilyType("payments")
                .apiVersion("3.0.0")
                .certificationStatus("Certified")
                .apiDiscoveryEndpoints(List.of(endpoint2))
                .build();

        AuthorisationServerDto authServer = AuthorisationServerDto.builder()
                .authorisationServerId("auth1")
                .organisationId("org1")
                .customerFriendlyName("Test Bank Auth")
                .apiResources(List.of(apiResource1, apiResource2))
                .build();

        return ParticipantDto.builder()
                .organisationId("org1")
                .status("Active")
                .organisationName("Test Bank")
                .legalEntityName("Test Bank Legal")
                .registrationNumber("12345678000195")
                .authorisationServers(List.of(authServer))
                .build();
    }
}
