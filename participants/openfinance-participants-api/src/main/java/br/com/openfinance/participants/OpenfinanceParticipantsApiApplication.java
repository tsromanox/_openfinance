package br.com.openfinance.participants;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@EnableCaching
@EnableScheduling
@SpringBootApplication
public class OpenfinanceParticipantsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenfinanceParticipantsApiApplication.class, args);
	}

/*	@Bean
	public WebClient webClient() {
		return WebClient.builder()
				.exchangeStrategies(ExchangeStrategies.builder()
						.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
						.build())
				.build();
	}*/
}
