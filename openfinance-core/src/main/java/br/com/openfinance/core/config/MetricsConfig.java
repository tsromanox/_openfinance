package br.com.openfinance.core.config;

import br.com.openfinance.core.metrics.OpenFinanceMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Timer apiCallTimer(MeterRegistry registry) {
        return Timer.builder("openfinance.api.calls")
                .description("API call duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    @Bean
    public OpenFinanceMetrics openFinanceMetrics(MeterRegistry registry) {
        return new OpenFinanceMetrics(registry);
    }
}
