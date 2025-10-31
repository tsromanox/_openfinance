package com.openfinance.receptor.examples;

import com.openfinance.receptor.application.service.InstitutionConfigService;
import com.openfinance.receptor.domain.model.InstitutionConfig;
import com.openfinance.receptor.domain.model.InstitutionConfig.CircuitState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Exemplo prático de uso das classes InstitutionConfig.
 * 
 * Este exemplo demonstra:
 * 1. Criação de novas instituições
 * 2. Atualização de consentimentos ativos e recálculo de limites
 * 3. Simulação de falhas e circuit breaker
 * 4. Consulta de estatísticas
 * 5. Integração com rate limiter
 * 
 * Para executar: remova o @Profile e rode a aplicação
 */
@Component
@Slf4j
// @Profile("demo") // Descomente para executar apenas em profile demo
public class InstitutionConfigUsageExample implements CommandLineRunner {
    
    private final InstitutionConfigService configService;
    
    public InstitutionConfigUsageExample(InstitutionConfigService configService) {
        this.configService = configService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("DEMONSTRAÇÃO: InstitutionConfig");
        log.info("========================================\n");
        
        // 1. Criar nova instituição
        demonstrateCreation();
        
        // 2. Atualizar consentimentos e ver recálculo de limites
        demonstrateConsentUpdate();
        
        // 3. Simular falhas e circuit breaker
        demonstrateCircuitBreaker();
        
        // 4. Consultar estatísticas
        demonstrateStatistics();
        
        // 5. Gerenciamento de circuit breaker
        demonstrateCircuitManagement();
        
        log.info("\n========================================");
        log.info("DEMONSTRAÇÃO CONCLUÍDA");
        log.info("========================================");
    }
    
    /**
     * Demonstra criação de nova instituição com valores padrão
     */
    private void demonstrateCreation() {
        log.info("1. CRIANDO NOVA INSTITUIÇÃO");
        log.info("----------------------------");
        
        InstitutionConfig newBank = InstitutionConfig.builder()
            .institutionId("999")
            .institutionName("Banco Demo")
            .cnpj("12345678000199")
            .baseUrl("https://api.bancodemo.com.br/open-banking")
            .withDefaults()  // Aplica valores padrão regulatórios
            .build();
        
        InstitutionConfig saved = configService.createConfig(newBank);
        
        log.info("✓ Instituição criada: {} (ID: {})", 
            saved.getInstitutionName(), saved.getInstitutionId());
        log.info("  - TPS Global: {}", saved.getGlobalTps());
        log.info("  - TPM Alta Freq: {}", saved.getHighFreqTpm());
        log.info("  - Circuit State: {}", saved.getCircuitState());
        log.info("  - Disponível: {}\n", saved.getIsAvailable());
    }
    
    /**
     * Demonstra atualização de consentimentos ativos e recálculo automático de limites
     */
    private void demonstrateConsentUpdate() {
        log.info("2. ATUALIZANDO CONSENTIMENTOS ATIVOS");
        log.info("-------------------------------------");
        
        String institutionId = "999";
        
        // Cenário 1: 500K consentimentos (faixa: <= 1M = 2.500 TPM)
        configService.updateActiveConsents(institutionId, 500_000L);
        InstitutionConfig config = configService.getConfig(institutionId);
        log.info("✓ 500K consentimentos → TPM: {} (esperado: 2.500)", config.getHighFreqTpm());
        
        // Cenário 2: 1.5M consentimentos (faixa: 1M-2M = 5.000 TPM)
        configService.updateActiveConsents(institutionId, 1_500_000L);
        config = configService.getConfig(institutionId);
        log.info("✓ 1.5M consentimentos → TPM: {} (esperado: 5.000)", config.getHighFreqTpm());
        
        // Cenário 3: 5M consentimentos (faixa: 3M-6M = 10.000 TPM)
        configService.updateActiveConsents(institutionId, 5_000_000L);
        config = configService.getConfig(institutionId);
        log.info("✓ 5M consentimentos → TPM: {} (esperado: 10.000)", config.getHighFreqTpm());
        
        // Cenário 4: 10M consentimentos (> 6M = 10.000 + 2.000 por 2M adicional)
        configService.updateActiveConsents(institutionId, 10_000_000L);
        config = configService.getConfig(institutionId);
        log.info("✓ 10M consentimentos → TPM: {} (esperado: 14.000)\n", config.getHighFreqTpm());
    }
    
    /**
     * Demonstra comportamento do circuit breaker com falhas simuladas
     */
    private void demonstrateCircuitBreaker() {
        log.info("3. SIMULANDO FALHAS E CIRCUIT BREAKER");
        log.info("--------------------------------------");
        
        String institutionId = "999";
        
        // Falhas 1-4: Circuit continua fechado
        for (int i = 1; i <= 4; i++) {
            configService.recordFailure(institutionId);
            InstitutionConfig config = configService.getConfig(institutionId);
            log.info("✓ Falha #{}: {} falhas totais, circuit: {}, disponível: {}",
                i, config.getFailureCount(), config.getCircuitState(), config.getIsAvailable());
        }
        
        // Falha 5: Circuit ABRE
        configService.recordFailure(institutionId);
        InstitutionConfig config = configService.getConfig(institutionId);
        log.info("⚠️  Falha #5: {} falhas totais, circuit: {}, disponível: {}",
            config.getFailureCount(), config.getCircuitState(), config.getIsAvailable());
        log.info("   Circuit aberto até: {}", config.getCircuitOpenUntil());
        
        // Recuperação: Registrar sucesso
        log.info("\n✓ Simulando recuperação da instituição...");
        configService.recordSuccess(institutionId);
        config = configService.getConfig(institutionId);
        log.info("✅ Sucesso registrado: circuit: {}, disponível: {}, falhas resetadas: {}\n",
            config.getCircuitState(), config.getIsAvailable(), config.getFailureCount());
    }
    
    /**
     * Demonstra consulta de estatísticas agregadas
     */
    private void demonstrateStatistics() {
        log.info("4. CONSULTANDO ESTATÍSTICAS");
        log.info("----------------------------");
        
        InstitutionConfigService.InstitutionStatistics stats = configService.getStatistics();
        
        log.info("Estatísticas do Sistema:");
        log.info("  - Total de Instituições: {}", stats.getTotalInstitutions());
        log.info("  - Disponíveis: {}", stats.getAvailableInstitutions());
        log.info("  - Indisponíveis: {}", stats.getUnavailableInstitutions());
        log.info("  - TPS Total Disponível: {}", stats.getTotalTps());
        log.info("  - Consentimentos Ativos: {}", stats.getTotalActiveConsents());
        log.info("  - Circuits Fechados: {}", stats.getCircuitsClosed());
        log.info("  - Circuits Abertos: {}", stats.getCircuitsOpen());
        log.info("  - Circuits Half-Open: {}\n", stats.getCircuitsHalfOpen());
    }
    
    /**
     * Demonstra gerenciamento manual de circuit breakers
     */
    private void demonstrateCircuitManagement() {
        log.info("5. GERENCIAMENTO DE CIRCUIT BREAKERS");
        log.info("-------------------------------------");
        
        String institutionId = "999";
        
        // Forçar abertura (útil para manutenção)
        log.info("⚠️  Forçando abertura de circuit para manutenção (5 minutos)...");
        configService.forceOpenCircuit(institutionId, 5);
        InstitutionConfig config = configService.getConfig(institutionId);
        log.info("✓ Circuit forçadamente aberto: {}, disponível: {}", 
            config.getCircuitState(), config.getIsAvailable());
        
        // Forçar fechamento (após manutenção)
        log.info("\n✓ Manutenção concluída, forçando fechamento...");
        configService.forceCloseCircuit(institutionId);
        config = configService.getConfig(institutionId);
        log.info("✅ Circuit forçadamente fechado: {}, disponível: {}\n", 
            config.getCircuitState(), config.getIsAvailable());
        
        // Listar instituições com problemas
        var problematic = configService.getInstitutionsWithProblems();
        log.info("Instituições com problemas (>= 3 falhas): {}", problematic.size());
        problematic.forEach(inst -> 
            log.info("  - {} ({}) - {} falhas", 
                inst.getInstitutionName(), inst.getInstitutionId(), inst.getFailureCount())
        );
    }
    
    /**
     * Exemplo de uso integrado com processamento de fila
     */
    public void exampleIntegrationWithQueueProcessing() {
        String institutionId = "341"; // Itaú
        
        // 1. Verificar se instituição está saudável
        if (!configService.isHealthy(institutionId)) {
            log.warn("Instituição {} não está saudável, pulando processamento", institutionId);
            return;
        }
        
        // 2. Buscar configuração
        InstitutionConfig config = configService.getConfig(institutionId);
        
        // 3. Usar configurações para timeout
        int connectionTimeout = config.getConnectionTimeoutSeconds();
        int readTimeout = config.getReadTimeoutSeconds();
        
        // 4. Calcular backoff para retry
        int attemptNumber = 2; // Segunda tentativa
        long backoffDelay = config.calculateBackoffDelay(attemptNumber);
        
        log.info("Processando instituição {}:", institutionId);
        log.info("  - Connection Timeout: {}s", connectionTimeout);
        log.info("  - Read Timeout: {}s", readTimeout);
        log.info("  - Backoff Delay (tentativa {}): {}ms", attemptNumber, backoffDelay);
        log.info("  - Max Tentativas: {}", config.getMaxRetryAttempts());
        
        try {
            // Simular chamada de API (substitua com sua lógica real)
            callExternalApi(config);
            
            // Registrar sucesso
            configService.recordSuccess(institutionId);
            
        } catch (Exception e) {
            // Registrar falha
            configService.recordFailure(institutionId);
            log.error("Erro ao processar instituição {}: {}", institutionId, e.getMessage());
            
            // Re-enfileirar com backoff
            // queueService.requeueWithDelay(job, Duration.ofMillis(backoffDelay));
        }
    }
    
    private void callExternalApi(InstitutionConfig config) {
        // Implementação da chamada real à API externa
        log.info("Chamando API: {}", config.getBaseUrl());
    }
    
    /**
     * Exemplo de atualização de limites em lote
     */
    public void exampleBatchRateLimitUpdate() {
        log.info("EXEMPLO: Atualização em Lote de Rate Limits");
        log.info("-------------------------------------------");
        
        // Cenário: Banco Central aumentou capacidade mínima de 300 para 450 TPS
        java.util.Map<String, InstitutionConfigService.RateLimitUpdate> updates = 
            new java.util.HashMap<>();
        
        updates.put("001", new InstitutionConfigService.RateLimitUpdate(600, 6000));
        updates.put("104", new InstitutionConfigService.RateLimitUpdate(600, 6000));
        updates.put("237", new InstitutionConfigService.RateLimitUpdate(600, 6000));
        updates.put("341", new InstitutionConfigService.RateLimitUpdate(700, 9000));
        
        configService.updateRateLimitsInBatch(updates);
        
        log.info("✓ Limites atualizados para {} instituições", updates.size());
    }
    
    /**
     * Exemplo de monitoramento contínuo
     */
    public void exampleContinuousMonitoring() {
        log.info("EXEMPLO: Monitoramento Contínuo");
        log.info("--------------------------------");
        
        // Buscar instituições disponíveis
        var available = configService.getAvailableInstitutions();
        log.info("Instituições disponíveis: {}", available.size());
        
        // Buscar top 5 com mais falhas
        var topFailing = configService.getTopFailingInstitutions(5);
        log.info("\nTop 5 Instituições com Mais Falhas:");
        topFailing.forEach(inst -> 
            log.info("  {}. {} - {} falhas (última: {})",
                topFailing.indexOf(inst) + 1,
                inst.getInstitutionName(),
                inst.getFailureCount(),
                inst.getLastFailure())
        );
        
        // Alertar sobre instituições críticas
        var problematic = configService.getInstitutionsWithProblems();
        if (!problematic.isEmpty()) {
            log.warn("\n⚠️  ALERTA: {} instituições com problemas!", problematic.size());
            problematic.forEach(inst ->
                log.warn("  - {} ({}): {} falhas consecutivas",
                    inst.getInstitutionName(),
                    inst.getInstitutionId(),
                    inst.getFailureCount())
            );
        }
    }
}
