package com.openfinance.receptor.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade que armazena configurações de rate limiting e circuit breaker
 * por instituição participante do Open Finance Brasil.
 * 
 * Segue especificações regulatórias do BCB para limites de TPS/TPM.
 */
@Entity
@Table(name = "institution_config", 
       indexes = {
           @Index(name = "idx_institution_available", columnList = "is_available"),
           @Index(name = "idx_institution_updated", columnList = "updated_at"),
           @Index(name = "idx_institution_circuit_state", columnList = "circuit_state"),
           @Index(name = "idx_institution_cnpj", columnList = "cnpj")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionConfig {
    
    // ========== IDENTIFICAÇÃO ==========
    
    @Id
    @Column(name = "institution_id", length = 100, nullable = false)
    private String institutionId;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "institution_name", nullable = false)
    private String institutionName;
    
    @Size(max = 14)
    @Column(name = "cnpj", length = 14, unique = true)
    private String cnpj;
    
    // ========== LIMITES REGULATÓRIOS (Open Finance Brasil) ==========
    
    /**
     * TPS Global mínimo obrigatório: 300 TPS
     * Aumenta em 150 TPS quando capacidade é atingida em 3 quinzenas consecutivas
     */
    @NotNull
    @Min(300)
    @Max(10000)
    @Column(name = "global_tps", nullable = false)
    @Builder.Default
    private Integer globalTps = 300;
    
    /**
     * TPM para endpoints de alta frequência (contas, saldos, transações)
     * Calculado dinamicamente baseado no número de consentimentos ativos:
     * - <= 1M: 2.500 TPM
     * - 1M-2M: 5.000 TPM
     * - 2M-3M: 8.000 TPM
     * - 3M-6M: 10.000 TPM
     * - > 6M: +2.000 TPM a cada 2M adicionais
     */
    @NotNull
    @Min(2500)
    @Column(name = "high_freq_tpm", nullable = false)
    @Builder.Default
    private Integer highFreqTpm = 2500;
    
    /**
     * TPM para endpoints de média-alta frequência: 2.000 TPM
     */
    @NotNull
    @Min(2000)
    @Column(name = "medium_high_freq_tpm", nullable = false)
    @Builder.Default
    private Integer mediumHighFreqTpm = 2000;
    
    /**
     * TPM para endpoints de média frequência: 1.500 TPM
     */
    @NotNull
    @Min(1500)
    @Column(name = "medium_freq_tpm", nullable = false)
    @Builder.Default
    private Integer mediumFreqTpm = 1500;
    
    /**
     * TPM para endpoints de baixa frequência: 1.000 TPM
     */
    @NotNull
    @Min(1000)
    @Column(name = "low_freq_tpm", nullable = false)
    @Builder.Default
    private Integer lowFreqTpm = 1000;
    
    /**
     * Número de consentimentos ativos (usado para calcular limites dinâmicos)
     */
    @Min(0)
    @Column(name = "active_consents_count")
    @Builder.Default
    private Long activeConsentsCount = 0L;
    
    // ========== CIRCUIT BREAKER STATE ==========
    
    /**
     * Indica se a instituição está disponível para processamento
     */
    @NotNull
    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;
    
    /**
     * Contador de falhas consecutivas
     */
    @Min(0)
    @Column(name = "failure_count")
    @Builder.Default
    private Integer failureCount = 0;
    
    /**
     * Última vez que uma falha ocorreu
     */
    @Column(name = "last_failure")
    private LocalDateTime lastFailure;
    
    /**
     * Circuit breaker aberto até este momento
     */
    @Column(name = "circuit_open_until")
    private LocalDateTime circuitOpenUntil;
    
    /**
     * Estado do circuit breaker
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "circuit_state", length = 20)
    @Builder.Default
    private CircuitState circuitState = CircuitState.CLOSED;
    
    // ========== CONFIGURAÇÕES DE RETRY ==========
    
    /**
     * Tempo de backoff inicial em milissegundos (padrão: 1 segundo)
     */
    @Min(100)
    @Max(60000)
    @Column(name = "initial_backoff_ms")
    @Builder.Default
    private Integer initialBackoffMs = 1000;
    
    /**
     * Multiplicador de backoff exponencial (padrão: 2.0)
     */
    @DecimalMin("1.0")
    @DecimalMax("5.0")
    @Column(name = "backoff_multiplier")
    @Builder.Default
    private Double backoffMultiplier = 2.0;
    
    /**
     * Backoff máximo em milissegundos (padrão: 5 minutos)
     */
    @Min(1000)
    @Max(3600000)
    @Column(name = "max_backoff_ms")
    @Builder.Default
    private Integer maxBackoffMs = 300000;
    
    /**
     * Número máximo de tentativas antes de mover para DLQ (padrão: 3)
     */
    @Min(1)
    @Max(10)
    @Column(name = "max_retry_attempts")
    @Builder.Default
    private Integer maxRetryAttempts = 3;
    
    // ========== CONFIGURAÇÕES DE TIMEOUT ==========
    
    /**
     * Timeout de conexão em segundos (padrão: 10s)
     */
    @Min(1)
    @Max(60)
    @Column(name = "connection_timeout_seconds")
    @Builder.Default
    private Integer connectionTimeoutSeconds = 10;
    
    /**
     * Timeout de leitura em segundos (padrão: 30s)
     */
    @Min(1)
    @Max(300)
    @Column(name = "read_timeout_seconds")
    @Builder.Default
    private Integer readTimeoutSeconds = 30;
    
    // ========== METADATA DA INSTITUIÇÃO ==========
    
    /**
     * URL base da API da instituição
     */
    @Size(max = 500)
    @Column(name = "base_url", length = 500)
    private String baseUrl;
    
    /**
     * Versão da API suportada (padrão: v2)
     */
    @Size(max = 20)
    @Column(name = "api_version", length = 20)
    @Builder.Default
    private String apiVersion = "v2";
    
    /**
     * Indica se a instituição requer autenticação mTLS
     */
    @Column(name = "requires_mtls")
    @Builder.Default
    private Boolean requiresMtls = true;
    
    /**
     * Observações/notas sobre a instituição
     */
    @Size(max = 1000)
    @Column(name = "notes", length = 1000)
    private String notes;
    
    // ========== AUDIT FIELDS ==========
    
    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @NotNull
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    
    // ========== JPA CALLBACKS ==========
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    // ========== MÉTODOS DE NEGÓCIO ==========
    
    /**
     * Calcula TPM de alta frequência baseado no número de consentimentos ativos.
     * Segue especificação do Open Finance Brasil.
     */
    public void updateHighFreqTpmBasedOnConsents() {
        if (activeConsentsCount == null) {
            this.highFreqTpm = 2500;
            return;
        }
        
        if (activeConsentsCount <= 1_000_000) {
            this.highFreqTpm = 2500;
        } else if (activeConsentsCount <= 2_000_000) {
            this.highFreqTpm = 5000;
        } else if (activeConsentsCount <= 3_000_000) {
            this.highFreqTpm = 8000;
        } else if (activeConsentsCount <= 6_000_000) {
            this.highFreqTpm = 10000;
        } else {
            // Adiciona 2000 TPM a cada 2 milhões acima de 6M
            long additionalMillions = (activeConsentsCount - 6_000_000) / 2_000_000;
            this.highFreqTpm = 10000 + (int)(additionalMillions * 2000);
        }
    }
    
    /**
     * Registra uma falha e atualiza o circuit breaker.
     * Abre o circuit após 5 falhas consecutivas.
     */
    public void recordFailure() {
        this.failureCount++;
        this.lastFailure = LocalDateTime.now();
        
        // Abre circuit após 5 falhas consecutivas
        if (this.failureCount >= 5) {
            this.circuitState = CircuitState.OPEN;
            this.isAvailable = false;
            // Circuit aberto por 5 minutos
            this.circuitOpenUntil = LocalDateTime.now().plusMinutes(5);
        }
    }
    
    /**
     * Registra sucesso e reseta contador de falhas.
     */
    public void recordSuccess() {
        this.failureCount = 0;
        this.lastFailure = null;
        this.circuitState = CircuitState.CLOSED;
        this.isAvailable = true;
        this.circuitOpenUntil = null;
    }
    
    /**
     * Verifica se o circuit breaker deve ser reaberto (transição para HALF_OPEN).
     * 
     * @return true se deve reabrir, false caso contrário
     */
    public boolean shouldReopenCircuit() {
        if (circuitState == CircuitState.OPEN && circuitOpenUntil != null) {
            if (LocalDateTime.now().isAfter(circuitOpenUntil)) {
                this.circuitState = CircuitState.HALF_OPEN;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Calcula o próximo delay de backoff com base no número de tentativas.
     * 
     * @param attemptNumber número da tentativa atual (1-based)
     * @return delay em milissegundos
     */
    public long calculateBackoffDelay(int attemptNumber) {
        long backoff = (long)(initialBackoffMs * Math.pow(backoffMultiplier, attemptNumber - 1));
        return Math.min(backoff, maxBackoffMs);
    }
    
    // ========== ENUM ==========
    
    /**
     * Estados possíveis do circuit breaker
     */
    public enum CircuitState {
        /**
         * Funcionando normalmente - todas as requisições são permitidas
         */
        CLOSED,
        
        /**
         * Bloqueado devido a falhas - requisições são rejeitadas
         */
        OPEN,
        
        /**
         * Em teste após período de espera - número limitado de requisições permitidas
         */
        HALF_OPEN
    }
    
    // ========== BUILDER CUSTOMIZADO ==========
    
    /**
     * Builder customizado com valores padrão inteligentes
     */
    public static class InstitutionConfigBuilder {
        /**
         * Cria um builder com valores padrão para uma nova instituição
         */
        public InstitutionConfigBuilder withDefaults() {
            this.globalTps = 300;
            this.highFreqTpm = 2500;
            this.mediumHighFreqTpm = 2000;
            this.mediumFreqTpm = 1500;
            this.lowFreqTpm = 1000;
            this.activeConsentsCount = 0L;
            this.isAvailable = true;
            this.failureCount = 0;
            this.circuitState = CircuitState.CLOSED;
            this.initialBackoffMs = 1000;
            this.backoffMultiplier = 2.0;
            this.maxBackoffMs = 300000;
            this.maxRetryAttempts = 3;
            this.connectionTimeoutSeconds = 10;
            this.readTimeoutSeconds = 30;
            this.apiVersion = "v2";
            this.requiresMtls = true;
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
            return this;
        }
    }
}
