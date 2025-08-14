// ParticipantController.java (atualizado com anotações OpenAPI)
package br.com.openfinance.participants.controller;

import br.com.openfinance.participants.dto.response.*;
import br.com.openfinance.participants.service.ParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/participants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Participantes", description = "API para consulta de participantes do Open Finance Brasil")
public class ParticipantController {

    private final ParticipantService participantService;

    @Operation(
            summary = "Buscar participante por CNPJ",
            description = "Retorna as informações completas de um participante do Open Finance Brasil com base no CNPJ informado. " +
                    "O CNPJ pode ser informado com ou sem formatação (pontos, barras e hífen).",
            tags = {"Participantes"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Participante encontrado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ParticipantResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Exemplo de resposta",
                                    value = """
                        {
                          "cnpj": "00000000000191",
                          "organisationId": "b961c4eb-509d-4edf-afeb-35642b38185d",
                          "organisationName": "Banco do Brasil S.A.",
                          "legalEntityName": "Banco do Brasil S.A.",
                          "status": "Active",
                          "apiEndpoints": {
                            "accounts": ["https://api.bb.com.br/open-banking/accounts/v2"],
                            "payments": ["https://api.bb.com.br/open-banking/payments/v3"]
                          },
                          "lastUpdated": "2024-01-15T10:30:00Z"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "CNPJ inválido ou mal formatado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                          "timestamp": "2024-01-15T10:30:00Z",
                          "status": 400,
                          "error": "Bad Request",
                          "message": "CNPJ deve conter 14 dígitos",
                          "path": "/api/v1/participants/123"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Participante não encontrado para o CNPJ informado"
            )
    })
    @GetMapping("/{cnpj}")
    public ResponseEntity<ParticipantResponseDto> getParticipantByCnpj(
            @Parameter(
                    description = "CNPJ do participante (com ou sem formatação)",
                    example = "00000000000191",
                    required = true
            )
            @PathVariable String cnpj) {

        log.info("Buscando participante por CNPJ: {}", cnpj);

        return participantService.getParticipantByCnpj(cnpj)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Buscar endpoints de API de um participante",
            description = "Retorna a lista de endpoints de API disponíveis para um participante específico. " +
                    "Opcionalmente, pode filtrar por família de API (accounts, payments, etc.).",
            tags = {"Endpoints"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Endpoints encontrados com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiEndpointResponseDto.class),
                            examples = @ExampleObject(
                                    value = """
                        [
                          {
                            "apiFamily": "accounts",
                            "version": "2.0.0",
                            "baseUrl": "https://api.bb.com.br",
                            "fullEndpoint": "https://api.bb.com.br/open-banking/accounts/v2",
                            "certificationStatus": "Certified"
                          }
                        ]
                        """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Nenhum endpoint encontrado para os critérios informados")
    })
    @GetMapping("/{cnpj}/endpoints")
    public ResponseEntity<List<ApiEndpointResponseDto>> getApiEndpoints(
            @Parameter(
                    description = "CNPJ do participante",
                    example = "00000000000191",
                    required = true
            )
            @PathVariable String cnpj,
            @Parameter(
                    description = "Família de API para filtrar (opcional)",
                    example = "accounts",
                    schema = @Schema(
                            type = "string",
                            allowableValues = {"accounts", "payments", "resources", "consents", "customers"}
                    )
            )
            @RequestParam(required = false) String apiFamily) {

        log.info("Buscando endpoints para CNPJ: {} e família: {}", cnpj, apiFamily);

        List<ApiEndpointResponseDto> endpoints = participantService.getApiEndpointsByCnpj(cnpj, apiFamily);

        if (endpoints.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(endpoints);
    }

    @Operation(
            summary = "Listar todos os participantes",
            description = "Retorna um resumo de todos os participantes ativos do Open Finance Brasil " +
                    "com informações básicas e contadores de endpoints.",
            tags = {"Participantes"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de participantes retornada com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ParticipantSummaryDto.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<List<ParticipantSummaryDto>> getAllParticipants() {
        log.info("Listando todos os participantes");

        List<ParticipantSummaryDto> participants = participantService.getAllParticipants();
        return ResponseEntity.ok(participants);
    }

    @Operation(
            summary = "Listar famílias de API disponíveis",
            description = "Retorna todas as famílias de API disponíveis nos participantes cadastrados " +
                    "(accounts, payments, resources, etc.).",
            tags = {"Endpoints"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Famílias de API retornadas com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        [
                          "accounts",
                          "payments", 
                          "resources",
                          "consents",
                          "customers"
                        ]
                        """
                            )
                    )
            )
    })
    @GetMapping("/api-families")
    public ResponseEntity<Set<String>> getAvailableApiFamilies() {
        log.info("Buscando famílias de API disponíveis");

        Set<String> families = participantService.getAvailableApiFamilies();
        return ResponseEntity.ok(families);
    }

    @Operation(
            summary = "Verificar status do cache",
            description = "Retorna informações sobre o estado atual do cache, incluindo número de participantes " +
                    "carregados, última atualização e saúde do sistema.",
            tags = {"Cache"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Status do cache retornado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                          "totalParticipants": 156,
                          "lastGlobalUpdate": "2024-01-15T10:30:00Z",
                          "cacheHealthy": true,
                          "availableApiFamilies": 8
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        log.info("Verificando status do cache");

        Map<String, Object> status = participantService.getCacheStatus();
        return ResponseEntity.ok(status);
    }

    @Operation(
            summary = "Forçar atualização do cache",
            description = "Força uma atualização imediata do cache, buscando novos dados da API oficial " +
                    "do diretório de participantes do Open Finance Brasil.",
            tags = {"Cache"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Cache atualizado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                          "status": "success",
                          "message": "Cache atualizado com sucesso"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno ao atualizar cache",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                          "status": "error",
                          "message": "Erro ao atualizar cache: Connection timeout"
                        }
                        """
                            )
                    )
            )
    })
    @PostMapping("/cache/refresh")
    public ResponseEntity<Map<String, String>> refreshCache() {
        log.info("Forçando atualização do cache");

        try {
            participantService.updateParticipantsCache();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Cache atualizado com sucesso"
            ));
        } catch (Exception e) {
            log.error("Erro ao atualizar cache", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Erro ao atualizar cache: " + e.getMessage()
            ));
        }
    }
}