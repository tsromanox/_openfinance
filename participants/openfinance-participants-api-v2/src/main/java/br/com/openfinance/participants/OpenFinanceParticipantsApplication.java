package br.com.openfinance.participants;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class OpenFinanceParticipantsApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenFinanceParticipantsApplication.class, args);
	}

	@Bean(name = "webClient2")
	public WebClient webClient() {
		// Configurar WebClient com buffer maior para resposta grande
		return WebClient.builder()
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
						.build())
				.build();
	}
}
