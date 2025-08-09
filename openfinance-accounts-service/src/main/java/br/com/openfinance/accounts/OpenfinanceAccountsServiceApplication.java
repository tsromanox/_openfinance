package br.com.openfinance.accounts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
		"br.com.openfinance.accounts",
		"br.com.openfinance.core"
})
@EnableScheduling
public class OpenfinanceAccountsServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(OpenfinanceAccountsServiceApplication.class, args);
	}
}
