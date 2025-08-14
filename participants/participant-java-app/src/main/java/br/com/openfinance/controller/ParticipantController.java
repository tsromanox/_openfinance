package br.com.openfinance.controller;


import br.com.openfinance.dto.ApiDiscoveryEndpoint;
import br.com.openfinance.service.ParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/participants")
public class ParticipantController {

    private final ParticipantService participantService;

    private final String apiFamilyType = "accounts"; // Define o tipo de família da API

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @Operation(
            summary = "Obtém a URL base dos recursos por CNPJ do participante",
            responses = {
                    @ApiResponse(responseCode = "200", description = "URL encontrada com sucesso"),
                    @ApiResponse(responseCode = "404", description = "CNPJ não encontrado")
            }
    )
    @GetMapping("/{cnpj}/base-url")
    public ResponseEntity<String> getBaseUrl(
            @Parameter(description = "CNPJ do participante") @PathVariable String cnpj) {
        return participantService.findParticipantByCnpj(cnpj)
                .flatMap(participant ->
                        participant.AuthorisationServers().stream()
                                .flatMap(authServer -> authServer.ApiResources().stream())
                                .flatMap(apiResource -> apiResource.ApiDiscoveryEndpoints().stream())
                                .map(ApiDiscoveryEndpoint::ApiEndpoint)
                                .findFirst())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Obtém a URL base por CNPJ e ApiFamilyType",
            responses = {
                    @ApiResponse(responseCode = "200", description = "URL encontrada com sucesso"),
                    @ApiResponse(responseCode = "404", description = "Participante ou ApiFamilyType não encontrados")
            }
    )
    @GetMapping("/{cnpj}/base-url2")
    public ResponseEntity<String> getBaseUrl2(
            @Parameter(description = "CNPJ do participante") @PathVariable String cnpj,
            @Parameter(description = "ApiFamilyType para filtro") @RequestParam String apiFamilyType) {
        return participantService.findApiEndpoint(cnpj, apiFamilyType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
