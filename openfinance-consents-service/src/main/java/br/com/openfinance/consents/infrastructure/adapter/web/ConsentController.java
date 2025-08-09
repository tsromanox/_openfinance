package br.com.openfinance.consents.infrastructure.adapter.web;

import br.com.openfinance.consents.application.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/consents/v3")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Consents", description = "Operações para criação, consulta, renovação e revogação do consentimento")
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping("/consents")
    @Operation(summary = "Criar novo pedido de consentimento",
            operationId = "consentsPostConsents",
            description = "Método para a criação de um novo consentimento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Consentimento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição mal formada",
                    content = @Content(schema = @Schema(implementation = ResponseError.class))),
            @ApiResponse(responseCode = "422", description = "A sintaxe da requisição está correta, mas não foi possível processar",
                    content = @Content(schema = @Schema(implementation = ResponseErrorUnprocessableEntity.class))),
            @ApiResponse(responseCode = "429", description = "Muitas requisições")
    })
    public Mono<ResponseEntity<ResponseConsent>> createConsent(
            @Valid @RequestBody CreateConsent createConsent,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "x-fapi-auth-date", required = false) String xFapiAuthDate,
            @RequestHeader(value = "x-fapi-customer-ip-address", required = false) String xFapiCustomerIpAddress,
            @RequestHeader("x-fapi-interaction-id") @Parameter(required = true,
                    description = "UUID de correlação",
                    example = "d78fc4e5-37ca-4da3-adf2-9b082bf92280") String xFapiInteractionId,
            @RequestHeader(value = "x-customer-user-agent", required = false) String xCustomerUserAgent) {

        log.info("Creating consent with interaction id {}", xFapiInteractionId);

        // Extract client ID from token
        String clientId = extractClientIdFromToken(authorization);

        return consentService.createConsent(createConsent, clientId, xFapiInteractionId)
                .map(consent -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header("x-fapi-interaction-id", xFapiInteractionId)
                        .body(consent))
                .doOnSuccess(response -> log.info("Consent created successfully"))
                .doOnError(error -> log.error("Error creating consent", error));
    }

    @GetMapping("/consents/{consentId}")
    @Operation(summary = "Obter detalhes do consentimento identificado por consentId",
            operationId = "consentsGetConsentsConsentId",
            description = "Método para obter detalhes do consentimento identificado por consentId.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consentimento consultado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Consentimento não encontrado")
    })
    public Mono<ResponseEntity<ResponseConsentRead>> getConsent(
            @PathVariable @Parameter(description = "ID do consentimento",
                    example = "urn:bancoex:C1DD33123") String consentId,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "x-fapi-auth-date", required = false) String xFapiAuthDate,
            @RequestHeader(value = "x-fapi-customer-ip-address", required = false) String xFapiCustomerIpAddress,
            @RequestHeader("x-fapi-interaction-id") String xFapiInteractionId,
            @RequestHeader(value = "x-customer-user-agent", required = false) String xCustomerUserAgent) {

        log.debug("Getting consent {} with interaction id {}", consentId, xFapiInteractionId);

        String clientId = extractClientIdFromToken(authorization);

        return consentService.getConsent(consentId, clientId)
                .map(consent -> ResponseEntity
                        .ok()
                        .header("x-fapi-interaction-id", xFapiInteractionId)
                        .body(consent))
                .defaultIfEmpty(ResponseEntity
                        .notFound()
                        .header("x-fapi-interaction-id", xFapiInteractionId)
                        .build());
    }

    @DeleteMapping("/consents/{consentId}")
    @Operation(summary = "Deletar / Revogar o consentimento identificado por consentId",
            operationId = "consentsDeleteConsentsConsentId",
            description = "Método para deletar / revogar o consentimento identificado por consentId.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Consentimento revogado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Consentimento não encontrado"),
            @ApiResponse(responseCode = "422", description = "Consentimento em status rejeitado")
    })
    public Mono<ResponseEntity<Void>> revokeConsent(
            @PathVariable String consentId,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "x-fapi-auth-date", required = false) String xFapiAuthDate,
            @RequestHeader(value = "x-fapi-customer-ip-address", required = false) String xFapiCustomerIpAddress,
            @RequestHeader("x-fapi-interaction-id") String xFapiInteractionId,
            @RequestHeader(value = "x-customer-user-agent", required = false) String xCustomerUserAgent) {

        log.info("Revoking consent {} with interaction id {}", consentId, xFapiInteractionId);

        String clientId = extractClientIdFromToken(authorization);

        return consentService.revokeConsent(consentId, clientId)
                .then(Mono.just(ResponseEntity
                        .noContent()
                        .<Void>header("x-fapi-interaction-id", xFapiInteractionId)
                        .build()))
                .doOnSuccess(v -> log.info("Consent {} revoked successfully", consentId));
    }

    @PostMapping("/consents/{consentId}/extends")
    @Operation(summary = "Renovar consentimento identificado por consentId",
            operationId = "consentsPostConsentsConsentIdExtends",
            description = "Método utilizado para renovação de consentimento do cliente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Renovação do consentimento finalizada com sucesso"),
            @ApiResponse(responseCode = "422", description = "Não foi possível processar a renovação")
    })
    public Mono<ResponseEntity<ResponseConsentExtensions>> extendConsent(
            @PathVariable String consentId,
            @Valid @RequestBody CreateConsentExtensions createConsentExtensions,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "x-fapi-auth-date", required = false) String xFapiAuthDate,
            @RequestHeader("x-fapi-customer-ip-address") String xFapiCustomerIpAddress,
            @RequestHeader("x-fapi-interaction-id") String xFapiInteractionId,
            @RequestHeader("x-customer-user-agent") String xCustomerUserAgent) {

        log.info("Extending consent {} with interaction id {}", consentId, xFapiInteractionId);

        // Extract user from authorization code token
        String userId = extractUserIdFromToken(authorization);

        return consentService.extendConsent(consentId, createConsentExtensions, userId,
                        xFapiCustomerIpAddress, xCustomerUserAgent)
                .map(response -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .header("x-fapi-interaction-id", xFapiInteractionId)
                        .body(response))
                .doOnSuccess(response -> log.info("Consent {} extended successfully", consentId));
    }

    @GetMapping("/consents/{consentId}/extensions")
    @Operation(summary = "Obter detalhes de extensões feitas no consentimento",
            operationId = "consentsGetConsentsConsentIdExtensions",
            description = "Método para obter histórico de extensões de consentimento identificado por consentId.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Renovações de consentimento consultado com sucesso")
    })
    public Mono<ResponseEntity<ResponseConsentReadExtensions>> getConsentExtensions(
            @PathVariable String consentId,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "x-fapi-auth-date", required = false) String xFapiAuthDate,
            @RequestHeader(value = "x-fapi-customer-ip-address", required = false) String xFapiCustomerIpAddress,
            @RequestHeader("x-fapi-interaction-id") String xFapiInteractionId,
            @RequestHeader(value = "x-customer-user-agent", required = false) String xCustomerUserAgent,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(name = "page-size", defaultValue = "25") Integer pageSize) {

        log.debug("Getting consent extensions for {} with interaction id {}", consentId, xFapiInteractionId);

        String clientId = extractClientIdFromToken(authorization);

        return consentService.getConsentExtensions(consentId, clientId, page, pageSize)
                .map(extensions -> ResponseEntity
                        .ok()
                        .header("x-fapi-interaction-id", xFapiInteractionId)
                        .body(extensions));
    }

    // Helper method to extract client ID from token
    private String extractClientIdFromToken(String authorization) {
        // Implementation depends on your OAuth2 setup
        // This is a placeholder
        return "client-id-from-token";
    }

    // Helper method to extract user ID from authorization code token
    private String extractUserIdFromToken(String authorization) {
        // Implementation depends on your OAuth2 setup
        // This is a placeholder
        return "user-id-from-token";
    }
}
