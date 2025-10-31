package com.openfinance.receptor.domain.repository;

import com.openfinance.receptor.domain.model.InstitutionConfig;
import com.openfinance.receptor.domain.model.InstitutionConfig.CircuitState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para gerenciar configurações de instituições participantes do Open Finance Brasil.
 * 
 * Fornece queries otimizadas para:
 * - Gerenciamento de circuit breakers
 * - Atualização de limites de rate limiting
 * - Monitoramento de saúde das instituições
 */
@Repository
public interface InstitutionConfigRepository extends JpaRepository<InstitutionConfig, String> {
    
    // ========== QUERIES DE DISPONIBILIDADE ==========
    
    /**
     * Busca todas as instituições disponíveis (circuit breaker fechado).
     * Usado pelo scheduler para processar apenas instituições ativas.
     * 
     * @return lista de instituições disponíveis
     */
    List<InstitutionConfig> findByIsAvailableTrue();
    
    /**
     * Busca todas as instituições indisponíveis.
     * Útil para relatórios e monitoramento.
     * 
     * @return lista de instituições indisponíveis
     */
    List<InstitutionConfig> findByIsAvailableFalse();
    
    /**
     * Busca instituições por estado do circuit breaker.
     * 
     * @param circuitState estado do circuit (CLOSED, OPEN, HALF_OPEN)
     * @return lista de instituições neste estado
     */
    List<InstitutionConfig> findByCircuitState(CircuitState circuitState);
    
    /**
     * Busca instituições que devem ter circuit breaker reaberto.
     * Retorna instituições em estado OPEN cujo tempo de espera já expirou.
     * 
     * @param now timestamp atual
     * @return lista de instituições para reabrir circuit
     */
    @Query("SELECT i FROM InstitutionConfig i WHERE " +
           "i.circuitState = 'OPEN' AND " +
           "i.circuitOpenUntil IS NOT NULL AND " +
           "i.circuitOpenUntil < :now")
    List<InstitutionConfig> findInstitutionsToReopenCircuit(@Param("now") LocalDateTime now);
    
    // ========== QUERIES DE BUSCA ==========
    
    /**
     * Busca instituição por CNPJ.
     * 
     * @param cnpj CNPJ da instituição (14 dígitos)
     * @return Optional com a instituição encontrada
     */
    Optional<InstitutionConfig> findByCnpj(String cnpj);
    
    /**
     * Busca instituições com alta taxa de falhas (acima do threshold).
     * Útil para alertas e monitoramento proativo.
     * 
     * @param threshold número mínimo de falhas
     * @return lista de instituições com problemas
     */
    @Query("SELECT i FROM InstitutionConfig i WHERE " +
           "i.failureCount >= :threshold AND " +
           "i.isAvailable = true")
    List<InstitutionConfig> findInstitutionsWithHighFailureRate(@Param("threshold") Integer threshold);
    
    /**
     * Busca instituições que não foram atualizadas recentemente.
     * Pode indicar instituições inativas ou com problemas de sincronização.
     * 
     * @param threshold data limite (ex: 7 dias atrás)
     * @return lista de instituições não atualizadas
     */
    @Query("SELECT i FROM InstitutionConfig i WHERE i.updatedAt < :threshold")
    List<InstitutionConfig> findStaleInstitutions(@Param("threshold") LocalDateTime threshold);
    
    /**
     * Busca instituições por faixa de TPS.
     * Útil para análises de capacidade e planejamento.
     * 
     * @param minTps TPS mínimo
     * @param maxTps TPS máximo
     * @return lista de instituições na faixa
     */
    @Query("SELECT i FROM InstitutionConfig i WHERE " +
           "i.globalTps >= :minTps AND i.globalTps <= :maxTps")
    List<InstitutionConfig> findByTpsRange(
        @Param("minTps") Integer minTps,
        @Param("maxTps") Integer maxTps
    );
    
    // ========== QUERIES DE AGREGAÇÃO ==========
    
    /**
     * Conta instituições disponíveis.
     * 
     * @return número de instituições disponíveis
     */
    @Query("SELECT COUNT(i) FROM InstitutionConfig i WHERE i.isAvailable = true")
    long countAvailableInstitutions();
    
    /**
     * Calcula TPS total disponível no sistema.
     * 
     * @return soma de TPS de todas instituições disponíveis
     */
    @Query("SELECT COALESCE(SUM(i.globalTps), 0) FROM InstitutionConfig i WHERE i.isAvailable = true")
    long calculateTotalAvailableTps();
    
    /**
     * Calcula total de consentimentos ativos no sistema.
     * 
     * @return soma de consentimentos de todas instituições
     */
    @Query("SELECT COALESCE(SUM(i.activeConsentsCount), 0) FROM InstitutionConfig i")
    long calculateTotalActiveConsents();
    
    // ========== UPDATES ATÔMICOS ==========
    
    /**
     * Atualiza contador de consentimentos ativos de forma atômica.
     * Útil para sincronização externa.
     * 
     * @param institutionId ID da instituição
     * @param count novo valor do contador
     * @param now timestamp da atualização
     * @return número de registros atualizados (0 ou 1)
     */
    @Modifying
    @Query("UPDATE InstitutionConfig i SET " +
           "i.activeConsentsCount = :count, " +
           "i.updatedAt = :now " +
           "WHERE i.institutionId = :institutionId")
    int updateActiveConsentsCount(
        @Param("institutionId") String institutionId,
        @Param("count") Long count,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Incrementa contador de falhas de forma atômica.
     * Deve ser usado em conjunto com lógica de circuit breaker.
     * 
     * @param institutionId ID da instituição
     * @param now timestamp da falha
     * @return número de registros atualizados
     */
    @Modifying
    @Query("UPDATE InstitutionConfig i SET " +
           "i.failureCount = i.failureCount + 1, " +
           "i.lastFailure = :now, " +
           "i.updatedAt = :now " +
           "WHERE i.institutionId = :institutionId")
    int incrementFailureCount(
        @Param("institutionId") String institutionId,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Reseta contador de falhas de forma atômica.
     * Usado quando instituição volta a funcionar normalmente.
     * 
     * @param institutionId ID da instituição
     * @param now timestamp do reset
     * @return número de registros atualizados
     */
    @Modifying
    @Query("UPDATE InstitutionConfig i SET " +
           "i.failureCount = 0, " +
           "i.lastFailure = NULL, " +
           "i.circuitState = 'CLOSED', " +
           "i.isAvailable = true, " +
           "i.circuitOpenUntil = NULL, " +
           "i.updatedAt = :now " +
           "WHERE i.institutionId = :institutionId")
    int resetFailureCount(
        @Param("institutionId") String institutionId,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Abre circuit breaker de forma atômica.
     * 
     * @param institutionId ID da instituição
     * @param openUntil timestamp até quando o circuit ficará aberto
     * @param now timestamp da abertura
     * @return número de registros atualizados
     */
    @Modifying
    @Query("UPDATE InstitutionConfig i SET " +
           "i.circuitState = 'OPEN', " +
           "i.isAvailable = false, " +
           "i.circuitOpenUntil = :openUntil, " +
           "i.updatedAt = :now " +
           "WHERE i.institutionId = :institutionId")
    int openCircuitBreaker(
        @Param("institutionId") String institutionId,
        @Param("openUntil") LocalDateTime openUntil,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Move circuit breaker para estado HALF_OPEN de forma atômica.
     * Permite testar se a instituição voltou a funcionar.
     * 
     * @param institutionId ID da instituição
     * @param now timestamp da mudança de estado
     * @return número de registros atualizados
     */
    @Modifying
    @Query("UPDATE InstitutionConfig i SET " +
           "i.circuitState = 'HALF_OPEN', " +
           "i.updatedAt = :now " +
           "WHERE i.institutionId = :institutionId")
    int moveToHalfOpen(
        @Param("institutionId") String institutionId,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Fecha circuit breaker de forma atômica (HALF_OPEN -> CLOSED).
     * 
     * @param institutionId ID da instituição
     * @param now timestamp do fechamento
     * @return número de registros atualizados
     */
    @Modifying
    @Query("UPDATE InstitutionConfig i SET " +
           "i.circuitState = 'CLOSED', " +
           "i.isAvailable = true, " +
           "i.circuitOpenUntil = NULL, " +
           "i.updatedAt = :now " +
           "WHERE i.institutionId = :institutionId AND i.circuitState = 'HALF_OPEN'")
    int closeCircuitBreaker(
        @Param("institutionId") String institutionId,
        @Param("now") LocalDateTime now
    );
    
    /**
     * Atualiza limites de rate limiting em lote.
     * Útil para ajustes regulatórios ou operacionais.
     * 
     * @param institutionId ID da instituição
     * @param globalTps novo limite de TPS global
     * @param highFreqTpm novo limite de TPM alta frequência
     * @param now timestamp da atualização
     * @return número de registros atualizados
     */
    @Modifying
    @Query("UPDATE InstitutionConfig i SET " +
           "i.globalTps = :globalTps, " +
           "i.highFreqTpm = :highFreqTpm, " +
           "i.updatedAt = :now " +
           "WHERE i.institutionId = :institutionId")
    int updateRateLimits(
        @Param("institutionId") String institutionId,
        @Param("globalTps") Integer globalTps,
        @Param("highFreqTpm") Integer highFreqTpm,
        @Param("now") LocalDateTime now
    );
    
    // ========== QUERIES NATIVAS PARA PERFORMANCE ==========
    
    /**
     * Busca estatísticas agregadas de circuit breakers usando query nativa.
     * Mais performático que agregar via JPA.
     * 
     * @return array com [total, closed, open, half_open]
     */
    @Query(value = """
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN circuit_state = 'CLOSED' THEN 1 ELSE 0 END) as closed,
            SUM(CASE WHEN circuit_state = 'OPEN' THEN 1 ELSE 0 END) as open,
            SUM(CASE WHEN circuit_state = 'HALF_OPEN' THEN 1 ELSE 0 END) as half_open
        FROM institution_config
        """, nativeQuery = true)
    Object[] getCircuitBreakerStatistics();
    
    /**
     * Busca top N instituições por número de falhas.
     * Útil para dashboards e alertas.
     * 
     * @param limit número máximo de resultados
     * @return lista de instituições ordenada por falhas (descendente)
     */
    @Query("SELECT i FROM InstitutionConfig i " +
           "WHERE i.failureCount > 0 " +
           "ORDER BY i.failureCount DESC, i.lastFailure DESC")
    List<InstitutionConfig> findTopInstitutionsByFailureCount(Pageable pageable);
}
