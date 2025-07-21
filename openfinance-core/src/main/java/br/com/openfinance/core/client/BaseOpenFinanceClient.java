package br.com.openfinance.core.client;

import br.com.openfinance.core.security.OAuth2TokenProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class BaseOpenFinanceClient {

    protected final WebClient webClient;
    protected final OAuth2TokenProvider tokenProvider;
    protected final CircuitBreaker circuitBreaker;
    protected final Retry retry;

    protected BaseOpenFinanceClient(
            WebClient.Builder webClientBuilder,
            OAuth2TokenProvider tokenProvider,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            String baseUrl,
            String serviceName) {

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();

        this.tokenProvider = tokenProvider;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        this.retry = retryRegistry.retry(serviceName);

        // Metrics
        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("Circuit breaker {} transitioned from {} to {}",
                                serviceName, event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()));
    }

    protected <T> Mono<T> executeRequest(Mono<T> request) {
        return tokenProvider.getToken()
                .flatMap(token -> request)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry));
    }
}
