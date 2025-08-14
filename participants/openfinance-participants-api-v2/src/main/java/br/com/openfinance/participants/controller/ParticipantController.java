package br.com.openfinance.participants.controller;

import br.com.openfinance.participants.dto.response.ApiEndpointResponseDto;
import br.com.openfinance.participants.dto.response.ErrorResponseDto;
import br.com.openfinance.participants.dto.response.ParticipantResponseDto;
import br.com.openfinance.participants.dto.response.ParticipantSummaryDto;
import br.com.openfinance.participants.service.ParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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
/*@RequestMapping("/api/v1")*/
@RequestMapping("/api/v1/participants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Participants", description = "API para consulta de participantes do Open Finance Brasil")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ParticipantController {

    private final ParticipantService participantService;

    @Operation(summary = "Buscar participante por CNPJ",
            description = "Retorna informações detalhadas de um participante específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Participante encontrado",
                    content = @Content(schema = @Schema(implementation = ParticipantResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Participante não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "CNPJ inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    @GetMapping("/{cnpj}")
    public ResponseEntity<ParticipantResponseDto> getParticipantByCnpj(
            @Parameter(description = "CNPJ do participante (com ou sem formatação)",
                    example = "00000000000191", required = true)
            @PathVariable String cnpj) {

        log.info("Buscando participante por CNPJ: {}", cnpj);

        return participantService.getParticipantByCnpj(cnpj)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Buscar endpoints de API por CNPJ",
            description = "Retorna lista de endpoints de API disponíveis para um participante")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Endpoints encontrados",
                    content = @Content(schema = @Schema(implementation = ApiEndpointResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Participante não encontrado")
    })
    @GetMapping("/{cnpj}/endpoints")
    public ResponseEntity<List<ApiEndpointResponseDto>> getApiEndpoints(
            @Parameter(description = "CNPJ do participante", example = "00000000000191", required = true)
            @PathVariable String cnpj,
            @Parameter(description = "Família da API para filtrar (opcional)", example = "accounts")
            @RequestParam(required = false) String apiFamily) {

        log.info("Buscando endpoints para CNPJ: {} e família: {}", cnpj, apiFamily);

        List<ApiEndpointResponseDto> endpoints = participantService.getApiEndpointsByCnpj(cnpj, apiFamily);

        if (endpoints.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(endpoints);
    }

    @Operation(summary = "Listar todos os participantes",
            description = "Retorna lista resumida de todos os participantes cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de participantes",
                    content = @Content(schema = @Schema(implementation = ParticipantSummaryDto.class)))
    })
    @GetMapping
    public ResponseEntity<List<ParticipantSummaryDto>> getAllParticipants() {
        log.info("Listando todos os participantes");

        List<ParticipantSummaryDto> participants = participantService.getAllParticipants();
        return ResponseEntity.ok(participants);
    }

    @Operation(summary = "Listar famílias de API disponíveis",
            description = "Retorna lista de todas as famílias de API disponíveis no ecossistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de famílias de API")
    })
    @GetMapping("/api-families")
    public ResponseEntity<Set<String>> getAvailableApiFamilies() {
        log.info("Buscando famílias de API disponíveis");

        Set<String> families = participantService.getAvailableApiFamilies();
        return ResponseEntity.ok(families);
    }

    @Operation(summary = "Status do cache",
            description = "Retorna informações sobre o status do cache e última atualização")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status do cache")
    })
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        log.info("Verificando status do cache");

        Map<String, Object> status = participantService.getCacheStatus();
        return ResponseEntity.ok(status);
    }

    @Operation(summary = "Forçar atualização do cache",
            description = "Força uma atualização imediata do cache de participantes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atualização iniciada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro ao atualizar cache")
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
