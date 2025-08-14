package br.com.openfinance.participants.controller;

import br.com.openfinance.participants.service.ParticipantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@Tag(name = "Health", description = "Health check endpoint")
public class HealthCheckController {

    private final ParticipantService participantService;

    @Operation(summary = "Health check", description = "Verifica o status da aplicação e do cache")
    @ApiResponse(responseCode = "200", description = "Aplicação saudável")
    @ApiResponse(responseCode = "503", description = "Aplicação com problemas")
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());

        // Adiciona informações do cache
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
