package br.com.openfinance.service;


import br.com.openfinance.dto.Participant;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ParticipantService {

    private final WebClient webClient;
    private List<Participant> cachedParticipants;

    public ParticipantService(@Value("${participant.api.base-url}") String baseUrl) {
        // FIX: Increase the buffer size to handle large JSON responses
        final int bufferSizeInBytes = 16 * 1024 * 1024; // 16 MB

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(bufferSizeInBytes))
                .build();
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .build();
    }

    @PostConstruct
    @Scheduled(fixedRate = 7200000) // 2 horas em milissegundos
    public void updateParticipants() {
        log.info("Starting scheduled update of participants directory...");
        cachedParticipants = webClient.get()
                .uri("/participants")
                .retrieve()
                .bodyToFlux(Participant.class)
                .collectList()
                .block();
    }

    @Cacheable("participants")
    public Optional<Participant> findParticipantByCnpj(String cnpj) {
        return cachedParticipants.stream()
                .filter(p -> p.RegistrationNumber().equals(cnpj))
                .findFirst();
    }

    @Cacheable("participants")
    public Optional<String> findApiEndpoint(String cnpj, String apiFamilyType) {
        return cachedParticipants.stream()
                .filter(p -> p.RegistrationNumber().equals(cnpj))
                .flatMap(p -> p.AuthorisationServers().stream())
                .flatMap(a -> a.ApiResources().stream())
                .filter(apiResource -> apiResource.ApiFamilyType().equalsIgnoreCase(apiFamilyType))
                .flatMap(apiResource -> apiResource.ApiDiscoveryEndpoints().stream())
                .map(endpoint -> endpoint.ApiEndpoint())
                .findFirst();
    }
}
