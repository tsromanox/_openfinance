package br.com.openfinance.participants.service;

import br.com.openfinance.participants.dto.*;
import br.com.openfinance.participants.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantService {

    private final WebClient webClient;

    @Value("${openfinance.participants.api.url:https://data.directory.openbankingbrasil.org.br/participants}")
    private String participantsApiUrl;

    @Value("${openfinance.participants.api.timeout:30}")
    private int timeoutSeconds;

    // Cache em memória dos participantes indexado por CNPJ
    private final Map<String, ParticipantDto> participantsByCnpj = new ConcurrentHashMap<>();

    // Cache adicional para busca rápida
    private final Map<String, LocalDateTime> lastUpdateMap = new ConcurrentHashMap<>();

    private LocalDateTime lastGlobalUpdate;

    // Pattern para extrair URL base dos endpoints
    private static final Pattern BASE_URL_PATTERN = Pattern.compile("(https?://[^/]+)");

    @PostConstruct
    public void init() {
        log.info("Inicializando serviço de participantes do Open Finance Brasil");
        updateParticipantsCache();
    }

    // Atualiza cache a cada 2 horas
    @Scheduled(fixedRate = 2 * 60 * 60 * 1000) // 2 horas em millisegundos
    public void scheduledUpdate() {
        log.info("Executando atualização agendada dos participantes");
        updateParticipantsCache();
    }

    public void updateParticipantsCache() {
        try {
            log.info("Buscando participantes da API: {}", participantsApiUrl);

            List<ParticipantDto> participants = webClient.get()
                    .uri(participantsApiUrl)
                    .retrieve()
                    .bodyToFlux(ParticipantDto.class)
                    .collectList()
                    .block();

            if (participants != null) {
                // Limpar cache atual
                participantsByCnpj.clear();

                // Atualizar cache com novos dados
                participants.forEach(participant -> {
                    String cnpj = extractCnpjFromParticipant(participant);
                    if (cnpj != null && !cnpj.isEmpty()) {
                        participantsByCnpj.put(cnpj, participant);
                        lastUpdateMap.put(cnpj, LocalDateTime.now());
                    }
                });

                lastGlobalUpdate = LocalDateTime.now();
                log.info("Cache atualizado com {} participantes", participantsByCnpj.size());
            }

        } catch (WebClientResponseException e) {
            log.error("Erro HTTP ao buscar participantes: {} - {}", e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar cache de participantes", e);
        }
    }

    private String extractCnpjFromParticipant(ParticipantDto participant) {
        // Tenta extrair CNPJ do RegistrationNumber (formato mais comum)
        if (participant.getRegistrationNumber() != null &&
                participant.getRegistrationNumber().matches("\\d{14}")) {
            return participant.getRegistrationNumber();
        }

        // Tenta extrair do RegistrationId
        if (participant.getRegistrationId() != null &&
                participant.getRegistrationId().matches("\\d{14}")) {
            return participant.getRegistrationId();
        }

        // Busca nas OrgDomainRoleClaims
        if (participant.getOrgDomainRoleClaims() != null) {
            for (OrganisationAuthorityClaimDto claim : participant.getOrgDomainRoleClaims()) {
                if (claim.getRegistrationId() != null &&
                        claim.getRegistrationId().matches("\\d{14}")) {
                    return claim.getRegistrationId();
                }
            }
        }

        log.warn("CNPJ não encontrado para participante: {}", participant.getOrganisationName());
        return null;
    }

    public Optional<ParticipantResponseDto> getParticipantByCnpj(String cnpj) {
        // Remove formatação do CNPJ
        String cleanCnpj = cnpj.replaceAll("[^0-9]", "");

        if (cleanCnpj.length() != 14) {
            log.warn("CNPJ inválido fornecido: {}", cnpj);
            return Optional.empty();
        }

        ParticipantDto participant = participantsByCnpj.get(cleanCnpj);
        if (participant == null) {
            log.info("Participante não encontrado para CNPJ: {}", cleanCnpj);
            return Optional.empty();
        }

        return Optional.of(buildParticipantResponse(participant, cleanCnpj));
    }

    public List<ApiEndpointResponseDto> getApiEndpointsByCnpj(String cnpj, String apiFamily) {
        String cleanCnpj = cnpj.replaceAll("[^0-9]", "");

        ParticipantDto participant = participantsByCnpj.get(cleanCnpj);
        if (participant == null) {
            return List.of();
        }

        List<ApiEndpointResponseDto> endpoints = new ArrayList<>();

        if (participant.getAuthorisationServers() != null) {
            for (AuthorisationServerDto authServer : participant.getAuthorisationServers()) {
                if (authServer.getApiResources() != null) {
                    for (ApiResourceDto apiResource : authServer.getApiResources()) {
                        // Filtrar por família da API se especificado
                        if (apiFamily != null && !apiFamily.isEmpty() &&
                                !apiFamily.equalsIgnoreCase(apiResource.getApiFamilyType())) {
                            continue;
                        }

                        if (apiResource.getApiDiscoveryEndpoints() != null) {
                            for (ApiDiscoveryEndpointDto endpoint : apiResource.getApiDiscoveryEndpoints()) {
                                endpoints.add(buildApiEndpointResponse(apiResource, endpoint));
                            }
                        }
                    }
                }
            }
        }

        return endpoints;
    }

    public List<ParticipantSummaryDto> getAllParticipants() {
        return participantsByCnpj.entrySet().stream()
                .map(entry -> ParticipantSummaryDto.builder()
                        .cnpj(entry.getKey())
                        .organisationName(entry.getValue().getOrganisationName())
                        .legalEntityName(entry.getValue().getLegalEntityName())
                        .status(entry.getValue().getStatus())
                        .totalApiEndpoints(countApiEndpoints(entry.getValue()))
                        .lastUpdated(lastUpdateMap.get(entry.getKey()))
                        .build())
                .sorted(Comparator.comparing(ParticipantSummaryDto::getOrganisationName))
                .collect(Collectors.toList());
    }

    public Set<String> getAvailableApiFamilies() {
        return participantsByCnpj.values().stream()
                .flatMap(participant ->
                        Optional.ofNullable(participant.getAuthorisationServers()).orElse(List.of()).stream())
                .flatMap(authServer ->
                        Optional.ofNullable(authServer.getApiResources()).orElse(List.of()).stream())
                .map(ApiResourceDto::getApiFamilyType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Map<String, Object> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalParticipants", participantsByCnpj.size());
        status.put("lastGlobalUpdate", lastGlobalUpdate);
        status.put("cacheHealthy", !participantsByCnpj.isEmpty());
        status.put("availableApiFamilies", getAvailableApiFamilies().size());

        return status;
    }

    private ParticipantResponseDto buildParticipantResponse(ParticipantDto participant, String cnpj) {
        Map<String, List<String>> apiEndpoints = new HashMap<>();

        if (participant.getAuthorisationServers() != null) {
            for (AuthorisationServerDto authServer : participant.getAuthorisationServers()) {
                if (authServer.getApiResources() != null) {
                    for (ApiResourceDto apiResource : authServer.getApiResources()) {
                        String familyType = apiResource.getApiFamilyType();
                        if (familyType != null && apiResource.getApiDiscoveryEndpoints() != null) {
                            List<String> endpoints = apiResource.getApiDiscoveryEndpoints().stream()
                                    .map(ApiDiscoveryEndpointDto::getApiEndpoint)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());

                            apiEndpoints.computeIfAbsent(familyType, k -> new ArrayList<>()).addAll(endpoints);
                        }
                    }
                }
            }
        }

        return ParticipantResponseDto.builder()
                .cnpj(cnpj)
                .organisationId(participant.getOrganisationId())
                .organisationName(participant.getOrganisationName())
                .legalEntityName(participant.getLegalEntityName())
                .status(participant.getStatus())
                .apiEndpoints(apiEndpoints)
                .lastUpdated(lastUpdateMap.get(cnpj))
                .build();
    }

    private ApiEndpointResponseDto buildApiEndpointResponse(ApiResourceDto apiResource,
                                                            ApiDiscoveryEndpointDto endpoint) {
        String baseUrl = extractBaseUrl(endpoint.getApiEndpoint());

        return ApiEndpointResponseDto.builder()
                .apiFamily(apiResource.getApiFamilyType())
                .version(apiResource.getApiVersion())
                .baseUrl(baseUrl)
                .fullEndpoint(endpoint.getApiEndpoint())
                .certificationStatus(apiResource.getCertificationStatus())
                .build();
    }

    private String extractBaseUrl(String fullUrl) {
        if (fullUrl == null) return null;

        Matcher matcher = BASE_URL_PATTERN.matcher(fullUrl);
        return matcher.find() ? matcher.group(1) : fullUrl;
    }

    private int countApiEndpoints(ParticipantDto participant) {
        if (participant.getAuthorisationServers() == null) return 0;

        return participant.getAuthorisationServers().stream()
                .mapToInt(authServer ->
                        Optional.ofNullable(authServer.getApiResources()).orElse(List.of()).stream()
                                .mapToInt(apiResource ->
                                        Optional.ofNullable(apiResource.getApiDiscoveryEndpoints())
                                                .map(List::size).orElse(0))
                                .sum())
                .sum();
    }
}
