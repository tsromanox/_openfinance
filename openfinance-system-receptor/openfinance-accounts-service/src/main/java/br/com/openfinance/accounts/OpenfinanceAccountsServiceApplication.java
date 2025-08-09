package br.com.openfinance.accounts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {
        "br.com.openfinance.accounts",
        "br.com.openfinance.core"
})
@EnableJpaRepositories
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
@EnableCaching
@EnableKafka
public class OpenfinanceAccountsServiceApplication {

	public static void main(String[] args) {
        // Enable Virtual Threads
        System.setProperty("jdk.virtualThreadScheduler.parallelism", "10000");
        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "100000");
        SpringApplication.run(OpenfinanceAccountsServiceApplication.class, args);
	}

}
