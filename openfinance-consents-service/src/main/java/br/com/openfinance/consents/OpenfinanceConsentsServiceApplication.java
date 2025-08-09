package br.com.openfinance.consents;

import br.com.openfinance.core.config.CoreConfiguration;
import br.com.openfinance.core.config.KafkaConfiguration;
import br.com.openfinance.core.config.RedisConfiguration;
import br.com.openfinance.core.config.WebClientConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@Import({
		CoreConfiguration.class,
		KafkaConfiguration.class,
		RedisConfiguration.class,
		WebClientConfiguration.class
})
public class OpenfinanceConsentsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpenfinanceConsentsServiceApplication.class, args);
	}

	@Bean
	public ExecutorService virtualThreadExecutor() {
		// Java 21 Virtual Threads para máxima concorrência
		return Executors.newVirtualThreadPerTaskExecutor();
	}

	@Bean
	public ForkJoinPool customForkJoinPool() {
		// Pool otimizado para operações paralelas
		return new ForkJoinPool(
				Runtime.getRuntime().availableProcessors() * 4,
				ForkJoinPool.defaultForkJoinWorkerThreadFactory,
				null,
				true
		);
	}
}