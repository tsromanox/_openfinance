package br.com.openfinance.participants.controller;

import br.com.openfinance.participants.dto.response.ApiEndpointResponseDto;
import br.com.openfinance.participants.dto.response.ParticipantResponseDto;
import br.com.openfinance.participants.dto.response.ParticipantSummaryDto;
import br.com.openfinance.participants.service.ParticipantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParticipantController.class)
class ParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private ParticipantService participantService;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class MockConfig {
        @Bean
        ParticipantService participantService(@Mock ParticipantService mock) {
            return mock;
        }
    }

    @Test
    void shouldGetParticipantByCnpj() throws Exception {
        // Given
        ParticipantResponseDto participant = createMockParticipantResponse();
        when(participantService.getParticipantByCnpj("12345678000195"))
                .thenReturn(Optional.of(participant));

        // When & Then
        mockMvc.perform(get("/api/v1/participants/12345678000195"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                //.andExpected(jsonPath("$.cnpj").value("12345678000195"))
                .andExpect(jsonPath("$.organisationName").value("Test Bank"));
    }

    @Test
    void shouldReturnNotFoundWhenParticipantNotExists() throws Exception {
        // Given
        when(participantService.getParticipantByCnpj(anyString()))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/v1/participants/99999999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetApiEndpoints() throws Exception {
        // Given
        List<ApiEndpointResponseDto> endpoints = List.of(
                ApiEndpointResponseDto.builder()
                        .apiFamily("accounts")
                        .version("2.0.0")
                        .baseUrl("https://api.testbank.com.br")
                        .fullEndpoint("https://api.testbank.com.br/open-banking/accounts/v2")
                        .certificationStatus("Certified")
                        .build()
        );

        when(participantService.getApiEndpointsByCnpj("12345678000195", "accounts"))
                .thenReturn(endpoints);

        // When & Then
        mockMvc.perform(get("/api/v1/participants/12345678000195/endpoints")
                        .param("apiFamily", "accounts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].apiFamily").value("accounts"))
                .andExpect(jsonPath("$[0].baseUrl").value("https://api.testbank.com.br"));
    }

    @Test
    void shouldGetAllParticipants() throws Exception {
        // Given
        List<ParticipantSummaryDto> participants = List.of(
                ParticipantSummaryDto.builder()
                        .cnpj("12345678000195")
                        .organisationName("Test Bank")
                        .status("Active")
                        .totalApiEndpoints(5)
                        .lastUpdated(LocalDateTime.now())
                        .build()
        );

        when(participantService.getAllParticipants()).thenReturn(participants);

        // When & Then
        mockMvc.perform(get("/api/v1/participants"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].cnpj").value("12345678000195"))
                .andExpect(jsonPath("$[0].organisationName").value("Test Bank"));
    }

    @Test
    void shouldGetAvailableApiFamilies() throws Exception {
        // Given
        Set<String> families = Set.of("accounts", "payments", "resources");
        when(participantService.getAvailableApiFamilies()).thenReturn(families);

        // When & Then
        mockMvc.perform(get("/api/v1/participants/api-families"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void shouldGetCacheStatus() throws Exception {
        // Given
        Map<String, Object> status = Map.of(
                "totalParticipants", 10,
                "cacheHealthy", true,
                "availableApiFamilies", 5
        );

        when(participantService.getCacheStatus()).thenReturn(status);

        // When & Then
        mockMvc.perform(get("/api/v1/participants/cache/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalParticipants").value(10))
                .andExpect(jsonPath("$.cacheHealthy").value(true));
    }

    @Test
    void shouldRefreshCache() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/participants/cache/refresh"))
                //.andExpected(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"));
    }

    private ParticipantResponseDto createMockParticipantResponse() {
        return ParticipantResponseDto.builder()
                .cnpj("12345678000195")
                .organisationId("org1")
                .organisationName("Test Bank")
                .legalEntityName("Test Bank Legal")
                .status("Active")
                .apiEndpoints(Map.of("accounts", List.of("https://api.testbank.com.br/accounts")))
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}
