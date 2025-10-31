package com.openfinance.receptor.application.service;

import com.openfinance.receptor.domain.model.InstitutionConfig;
import com.openfinance.receptor.domain.model.InstitutionConfig.CircuitState;
import com.openfinance.receptor.domain.repository.InstitutionConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service para gerenciar configurações de instituições do Open Finance Brasil.
 * 
 * Responsabilidades:
 * - Cache de configurações (5 minutos)
 * - Gerenciamento de circuit breakers
 * - Atualização de limites de rate limiting
 * - Sincronização de consentimentos ativos
 * - Monitoramento de saúde das instituições
 */
@Service
@Slf4j
@Transactional
public class InstitutionConfigService {
    
    @Autowired
    private InstitutionConfigRepository repository;
    
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final int CIRCUIT_OPEN_DURATION_MINUTES = 5;
    private static final int HIGH_FAILURE_RATE_THRESHOLD = 3;
    
    // ========== OPERAÇÕES BÁSICAS ==========
    
    /**
     * Busca configuração com cache (expira após 5 minutos).
     * Cache evitará consultas repetidas ao banco durante processamento em massa.
     * 
     * @param institutionId ID da instituição
     * @return configuração da instituição
     * @throws IllegalArgumentException se instituição não existir
     */
    @Cacheable(value = "institutionConfig", key = "#institutionId")
    public InstitutionConfig getConfig(String institutionId) {
        return repository.findById(institutionId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Institution not found: " + institutionId));
    }
    
    /**
     * Busca configuração sem cache.
     * Útil quando precisa de dados em tempo real (ex: dashboards).
     * 
     * @param institutionId ID da instituição
     * @return Optional com a configuração
     */
    public Optional<InstitutionConfig> getConfigUncached(String institutionId) {
        return repository.findById(institutionId);
    }
    
    /**
     * Cria nova configuração de instituição.
     * 
     * @param config configuração a criar
     * @return configuração criada
     */
    public InstitutionConfig createConfig(InstitutionConfig config) {
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        
        // Calcula TPM inicial baseado em consentimentos
        config.updateHighFreqTpmBasedOnConsents();
        
        InstitutionConfig saved = repository.save(config);
        log.info("Configuração criada para instituição: {} ({})", 
            saved.getInstitutionId(), saved.getInstitutionName());
        
        return saved;
    }
    
    /**
     * Atualiza configuração e invalida cache.
     * 
     * @param config configuração atualizada
     * @return configuração salva
     */
    @CacheEvict(value = "institutionConfig", key = "#config.institutionId")
    public InstitutionConfig updateConfig(InstitutionConfig config) {
        config.setUpdatedAt(LocalDateTime.now());
        
        InstitutionConfig saved = repository.save(config);
        log.debug("Configuração atualizada para instituição: {}", config.getInstitutionId());
        
        return saved;
    }
    
    /**
     * Deleta configuração (use com cuidado - apenas para testes/migração).
     * 
     * @param institutionId ID da instituição
     */
    @CacheEvict(value = "institutionConfig", key = "#institutionId")
    public void deleteConfig(String institutionId) {
        repository.deleteById(institutionId);
        log.warn("Configuração DELETADA para instituição: {}", institutionId);
    }
    
    // ========== GERENCIAMENTO DE CIRCUIT BREAKER ==========
    
    /**
     * Registra falha na instituição e atualiza circuit breaker.
     * Abre o circuit após CIRCUIT_BREAKER_THRESHOLD falhas consecutivas.
     * 
     * @param institutionId ID da instituição
     */
    @CacheEvict(value = "institutionConfig", key = "#institutionId")
    public void recordFailure(String institutionId) {
        InstitutionConfig config = getConfigUncached(institutionId)
            .orElseThrow(() -> new IllegalArgumentException("Institution not found: " + institutionId));
        
        config.recordFailure();
        repository.save(config);
        
        log.warn("Falha registrada para instituição {}: {} falhas consecutivas (estado: {})", 
            institutionId, config.getFailureCount(), config.getCircuitState());
        
        if (config.getCircuitState() == CircuitState.OPEN) {
            log.error("⚠️ CIRCUIT BREAKER ABERTO para instituição {} até {}", 
                institutionId, config.getCircuitOpenUntil());
        }
    }
    
    /**
     * Registra sucesso e reseta contador de falhas.
     * Fecha o circuit breaker se estava aberto.
     * 
     * @param institutionId ID da instituição
     */
    @CacheEvict(value = "institutionConfig", key = "#institutionId")
    public void recordSuccess(String institutionId) {
        InstitutionConfig config = getConfigUncached(institutionId)
            .orElseThrow(() -> new IllegalArgumentException("Institution not found: " + institutionId));
        
        CircuitState previousState = config.getCircuitState();
        config.recordSuccess();
        repository.save(config);
        
        if (previousState == CircuitState.OPEN || previousState == CircuitState.HALF_OPEN) {
            log.info("✅ Circuit breaker FECHADO para instituição {} (era: {})", 
                institutionId, previousState);
        } else {
            log.debug("Sucesso registrado para instituição {}", institutionId);
        }
    }
    
    /**
     * Força abertura de circuit breaker (para manutenção ou testes).
     * 
     * @param institutionId ID da instituição
     * @param durationMinutes duração em minutos
     */
    @CacheEvict(value = "institutionConfig", key = "#institutionId")
    public void forceOpenCircuit(String institutionId, int durationMinutes) {
        LocalDateTime openUntil = LocalDateTime.now().plusMinutes(durationMinutes);
        repository.openCircuitBreaker(institutionId, openUntil, LocalDateTime.now());
        
        log.warn("⚠️ Circuit breaker FORÇADAMENTE ABERTO para {} por {} minutos (até {})", 
            institutionId, durationMinutes, openUntil);
    }
    
    /**
     * Força fechamento de circuit breaker.
     * 
     * @param institutionId ID da instituição
     */
    @CacheEvict(value = "institutionConfig", key = "#institutionId")
    public void forceCloseCircuit(String institutionId) {
        repository.resetFailureCount(institutionId, LocalDateTime.now());
        log.info("✅ Circuit breaker FORÇADAMENTE FECHADO para {}", institutionId);
    }
    
    /**
     * Job agendado para reabrir circuit breakers que já expiraram.
     * Executa a cada 30 segundos.
     */
    @Scheduled(fixedDelay = 30000)
    public void reopenExpiredCircuitBreakers() {
        List<InstitutionConfig> toReopen = repository
            .findInstitutionsToReopenCircuit(LocalDateTime.now());
        
        if (toReopen.isEmpty()) {
            return;
        }
        
        for (InstitutionConfig config : toReopen) {
            repository.moveToHalfOpen(config.getInstitutionId(), LocalDateTime.now());
            
            // Invalida cache
            evictCache(config.getInstitutionId());
            
            log.info("🔄 Circuit breaker movido para HALF_OPEN: {} (tentando recuperação)", 
                config.getInstitutionId());
        }
        
        log.info("Reabertos {} circuit breakers expirados", toReopen.size());
    }
    
    // ========== GERENCIAMENTO DE CONSENTIMENTOS ==========
    
    /**
     * Atualiza número de consentimentos ativos e recalcula limites de TPM.
     * 
     * @param institutionId ID da instituição
     * @param count novo número de consentimentos ativos
     */
    @CacheEvict(value = "institutionConfig", key = "#institutionId")
    public void updateActiveConsents(String institutionId, Long count) {
        InstitutionConfig config = getConfigUncached(institutionId)
            .orElseThrow(() -> new IllegalArgumentException("Institution not found: " + institutionId));
        
        Long oldCount = config.getActiveConsentsCount();
        Integer oldTpm = config.getHighFreqTpm();
        
        config.setActiveConsentsCount(count);
        config.updateHighFreqTpmBasedOnConsents();
        repository.save(config);
        
        if (!oldTpm.equals(config.getHighFreqTpm())) {
            log.info("📊 Limites atualizados para {}: {} consentimentos (TPM: {} -> {})",
                institutionId, count, oldTpm, config.getHighFreqTpm());
        } else {
            log.debug("Consentimentos ativos atualizados para {}: {} -> {}", 
                institutionId, oldCount, count);
        }
    }
    
    /**
     * Incrementa contador de consentimentos ativos.
     * 
     * @param institutionId ID da instituição
     * @param increment valor a incrementar
     */
    @CacheEvict(value = "institutionConfig", key = "#institutionId")
    public void incrementActiveConsents(String institutionId, Long increment) {
        InstitutionConfig config = getConfigUncached(institutionId)
            .orElseThrow(() -> new IllegalArgumentException("Institution not found: " + institutionId));
        
        Long newCount = config.getActiveConsentsCount() + increment;
        updateActiveConsents(institutionId, newCount);
    }
    
    // ========== CONSULTAS E MONITORAMENTO ==========
    
    /**
     * Busca todas as instituições disponíveis (circuit fechado).
     * 
     * @return lista de instituições disponíveis
     */
    public List<InstitutionConfig> getAvailableInstitutions() {
        return repository.findByIsAvailableTrue();
    }
    
    /**
     * Busca todas as instituições indisponíveis.
     * 
     * @return lista de instituições indisponíveis
     */
    public List<InstitutionConfig> getUnavailableInstitutions() {
        return repository.findByIsAvailableFalse();
    }
    
    /**
     * Busca instituições com alta taxa de falhas que precisam atenção.
     * 
     * @return lista de instituições problemáticas
     */
    public List<InstitutionConfig> getInstitutionsWithProblems() {
        return repository.findInstitutionsWithHighFailureRate(HIGH_FAILURE_RATE_THRESHOLD);
    }
    
    /**
     * Busca top N instituições por número de falhas.
     * 
     * @param limit número máximo de resultados
     * @return lista ordenada por falhas (descendente)
     */
    public List<InstitutionConfig> getTopFailingInstitutions(int limit) {
        return repository.findTopInstitutionsByFailureCount(PageRequest.of(0, limit));
    }
    
    /**
     * Calcula estatísticas agregadas do sistema.
     * 
     * @return estatísticas de instituições
     */
    public InstitutionStatistics getStatistics() {
        long total = repository.count();
        long available = repository.countAvailableInstitutions();
        long totalTps = repository.calculateTotalAvailableTps();
        long totalConsents = repository.calculateTotalActiveConsents();
        
        Object[] circuitStats = repository.getCircuitBreakerStatistics();
        
        return InstitutionStatistics.builder()
            .totalInstitutions(total)
            .availableInstitutions(available)
            .unavailableInstitutions(total - available)
            .totalTps(totalTps)
            .totalActiveConsents(totalConsents)
            .circuitsClosed(getLongFromArray(circuitStats, 1))
            .circuitsOpen(getLongFromArray(circuitStats, 2))
            .circuitsHalfOpen(getLongFromArray(circuitStats, 3))
            .build();
    }
    
    // ========== ATUALIZAÇÃO EM LOTE ==========
    
    /**
     * Atualiza limites de rate limiting para uma instituição.
     * 
     * @param institutionId ID da instituição
     * @param globalTps novo limite de TPS global
     * @param highFreqTpm novo limite de TPM alta frequência
     */
    @CacheEvict(value = "institutionConfig", key = "#institutionId")
    public void updateRateLimits(String institutionId, Integer globalTps, Integer highFreqTpm) {
        repository.updateRateLimits(institutionId, globalTps, highFreqTpm, LocalDateTime.now());
        log.info("Rate limits atualizados para {}: TPS={}, TPM={}", 
            institutionId, globalTps, highFreqTpm);
    }
    
    /**
     * Atualiza limites de rate limiting em lote para múltiplas instituições.
     * 
     * @param updates mapa de institutionId -> novos limites
     */
    public void updateRateLimitsInBatch(java.util.Map<String, RateLimitUpdate> updates) {
        updates.forEach((institutionId, update) -> {
            updateRateLimits(institutionId, update.getGlobalTps(), update.getHighFreqTpm());
        });
        
        log.info("Rate limits atualizados em lote para {} instituições", updates.size());
    }
    
    // ========== UTILIDADES ==========
    
    /**
     * Invalida cache de uma instituição específica.
     * 
     * @param institutionId ID da instituição
     */
    @CacheEvict(value = "institutionConfig", key = "#institutionId")
    public void evictCache(String institutionId) {
        log.debug("Cache invalidado para instituição: {}", institutionId);
    }
    
    /**
     * Invalida todo o cache de configurações.
     * Usar com cuidado - pode causar pico de consultas ao banco.
     */
    @CacheEvict(value = "institutionConfig", allEntries = true)
    public void evictAllCache() {
        log.warn("TODO o cache de instituições foi invalidado");
    }
    
    /**
     * Verifica se uma instituição está saudável para processamento.
     * 
     * @param institutionId ID da instituição
     * @return true se disponível e com poucos erros
     */
    public boolean isHealthy(String institutionId) {
        InstitutionConfig config = getConfig(institutionId);
        return config.getIsAvailable() && 
               config.getCircuitState() == CircuitState.CLOSED &&
               config.getFailureCount() < HIGH_FAILURE_RATE_THRESHOLD;
    }
    
    private Long getLongFromArray(Object[] array, int index) {
        if (array == null || array.length <= index || array[index] == null) {
            return 0L;
        }
        return ((Number) array[index]).longValue();
    }
    
    // ========== CLASSES AUXILIARES ==========
    
    /**
     * Estatísticas agregadas de instituições
     */
    @lombok.Data
    @lombok.Builder
    public static class InstitutionStatistics {
        private Long totalInstitutions;
        private Long availableInstitutions;
        private Long unavailableInstitutions;
        private Long totalTps;
        private Long totalActiveConsents;
        private Long circuitsClosed;
        private Long circuitsOpen;
        private Long circuitsHalfOpen;
    }
    
    /**
     * DTO para atualização de limites em lote
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class RateLimitUpdate {
        private Integer globalTps;
        private Integer highFreqTpm;
    }
}
