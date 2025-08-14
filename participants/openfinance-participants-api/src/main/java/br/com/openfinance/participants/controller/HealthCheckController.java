package br.com.openfinance.participants.controller;

import br.com.openfinance.participants.service.ParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Sistema", description = "Endpoints para verificação da saúde do sistema")
public class HealthCheckController {

    private final ParticipantService participantService;

    @Operation(
            summary = "Verificar saúde do sistema",
            description = "Endpoint para verificação da saúde geral do sistema, incluindo status do cache " +
                    "e conectividade com serviços externos.",
            tags = {"Sistema"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Sistema saudável",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                          "status": "UP",
                          "timestamp": "2024-01-15T10:30:00Z",
                          "cache": {
                            "totalParticipants": 156,
                            "cacheHealthy": true,
                            "availableApiFamilies": 8
                          }
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Sistema com problemas",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                        {
                          "status": "DOWN",
                          "timestamp": "2024-01-15T10:30:00Z",
                          "cache": {
                            "totalParticipants": 0,
                            "cacheHealthy": false,
                            "availableApiFamilies": 0
                          }
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());

        Map<String, Object> cacheStatus = participantService.getCacheStatus();
        health.put("cache", cacheStatus);

        // Verifica se o cache está saudável
        boolean cacheHealthy = (Boolean) cacheStatus.get("cacheHealthy");
        if (!cacheHealthy) {
            health.put("status", "DOWN");
            return ResponseEntity.status(503).body(health);
        }

        return ResponseEntity.ok(health);
    }
}

