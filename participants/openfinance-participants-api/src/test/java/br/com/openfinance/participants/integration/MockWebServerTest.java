package br.com.openfinance.participants.integration;

import br.com.openfinance.participants.dto.ParticipantDto;
import br.com.openfinance.participants.service.ParticipantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MockWebServerTest {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Update service URL to point to mock server
        String baseUrl = String.format("http://localhost:%s/participants",
                mockWebServer.getPort());
        ReflectionTestUtils.setField(participantService, "participantsApiUrl", baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldUpdateCacheFromMockServer() throws Exception {
        // Given
        ParticipantDto mockParticipant = ParticipantDto.builder()
                .organisationId("org1")
                .organisationName("Mock Bank")
                .registrationNumber("12345678000195")
                .status("Active")
                .build();

        String responseBody = objectMapper.writeValueAsString(List.of(mockParticipant));

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        // When
        participantService.updateParticipantsCache();

        // Then
        var participant = participantService.getParticipantByCnpj("12345678000195");
        assertThat(participant).isPresent();
        assertThat(participant.get().getOrganisationName()).isEqualTo("Mock Bank");
    }
}
