package br.com.openfinance.participants;

import br.com.openfinance.participants.controller.ParticipantController;
import br.com.openfinance.participants.service.ParticipantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OpenFinanceParticipantsApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ParticipantController participantController;

	@Autowired
	private ParticipantService participantService;

	@Test
	void contextLoads() {
		assertThat(participantController).isNotNull();
		assertThat(participantService).isNotNull();
	}

	@Test
	void healthEndpointShouldBeAccessible() {
		String response = this.restTemplate.getForObject(
				"http://localhost:" + port + "/health", String.class);
		assertThat(response).contains("UP");
	}

	@Test
	void participantsEndpointShouldBeAccessible() {
		String response = this.restTemplate.getForObject(
				"http://localhost:" + port + "/api/v1/participants", String.class);
		assertThat(response).isNotNull();
	}
}
