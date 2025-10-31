package com.openfinance.receptor.domain.model;

import com.openfinance.receptor.domain.model.InstitutionConfig.CircuitState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para InstitutionConfig
 */
@DisplayName("InstitutionConfig Tests")
class InstitutionConfigTest {
    
    // ========== TESTES DE CÁLCULO DE TPM ==========
    
    @Nested
    @DisplayName("Cálculo de High Frequency TPM")
    class HighFreqTpmCalculationTests {
        
        @Test
        @DisplayName("Deve calcular 2.500 TPM para até 1 milhão de consentimentos")
        void shouldCalculate2500TpmFor1Million() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .institutionName("Banco Teste")
                .activeConsentsCount(1_000_000L)
                .build();
            
            config.updateHighFreqTpmBasedOnConsents();
            
            assertEquals(2500, config.getHighFreqTpm());
        }
        
        @Test
        @DisplayName("Deve calcular 2.500 TPM para menos de 1 milhão de consentimentos")
        void shouldCalculate2500TpmForUnder1Million() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .activeConsentsCount(500_000L)
                .build();
            
            config.updateHighFreqTpmBasedOnConsents();
            
            assertEquals(2500, config.getHighFreqTpm());
        }
        
        @Test
        @DisplayName("Deve calcular 5.000 TPM para 1-2 milhões de consentimentos")
        void shouldCalculate5000TpmFor1to2Million() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .activeConsentsCount(1_500_000L)
                .build();
            
            config.updateHighFreqTpmBasedOnConsents();
            
            assertEquals(5000, config.getHighFreqTpm());
        }
        
        @Test
        @DisplayName("Deve calcular 8.000 TPM para 2-3 milhões de consentimentos")
        void shouldCalculate8000TpmFor2to3Million() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .activeConsentsCount(2_500_000L)
                .build();
            
            config.updateHighFreqTpmBasedOnConsents();
            
            assertEquals(8000, config.getHighFreqTpm());
        }
        
        @Test
        @DisplayName("Deve calcular 10.000 TPM para 3-6 milhões de consentimentos")
        void shouldCalculate10000TpmFor3to6Million() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .activeConsentsCount(5_000_000L)
                .build();
            
            config.updateHighFreqTpmBasedOnConsents();
            
            assertEquals(10000, config.getHighFreqTpm());
        }
        
        @Test
        @DisplayName("Deve calcular 14.000 TPM para 10 milhões de consentimentos")
        void shouldCalculate14000TpmFor10Million() {
            // 6M = 10.000 TPM
            // Cada 2M adicional = +2.000 TPM
            // 10M - 6M = 4M = 2 blocos de 2M = +4.000 TPM
            // Total: 10.000 + 4.000 = 14.000 TPM
            
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .activeConsentsCount(10_000_000L)
                .build();
            
            config.updateHighFreqTpmBasedOnConsents();
            
            assertEquals(14000, config.getHighFreqTpm());
        }
        
        @Test
        @DisplayName("Deve calcular 20.000 TPM para 18 milhões de consentimentos")
        void shouldCalculate20000TpmFor18Million() {
            // 18M - 6M = 12M = 6 blocos de 2M = +12.000 TPM
            // Total: 10.000 + 12.000 = 22.000 TPM
            
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .activeConsentsCount(18_000_000L)
                .build();
            
            config.updateHighFreqTpmBasedOnConsents();
            
            assertEquals(22000, config.getHighFreqTpm());
        }
        
        @Test
        @DisplayName("Deve tratar null como 0 consentimentos")
        void shouldHandleNullAsZeroConsents() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .activeConsentsCount(null)
                .build();
            
            config.updateHighFreqTpmBasedOnConsents();
            
            assertEquals(2500, config.getHighFreqTpm());
        }
    }
    
    // ========== TESTES DE CIRCUIT BREAKER ==========
    
    @Nested
    @DisplayName("Circuit Breaker Behavior")
    class CircuitBreakerTests {
        
        @Test
        @DisplayName("Deve iniciar com circuit breaker CLOSED")
        void shouldStartWithClosedCircuit() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .build();
            
            assertEquals(CircuitState.CLOSED, config.getCircuitState());
            assertTrue(config.getIsAvailable());
            assertEquals(0, config.getFailureCount());
        }
        
        @Test
        @DisplayName("Deve incrementar contador de falhas")
        void shouldIncrementFailureCount() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .build();
            
            config.recordFailure();
            
            assertEquals(1, config.getFailureCount());
            assertNotNull(config.getLastFailure());
            assertEquals(CircuitState.CLOSED, config.getCircuitState());
            assertTrue(config.getIsAvailable());
        }
        
        @Test
        @DisplayName("Deve manter circuit CLOSED até 4 falhas")
        void shouldKeepCircuitClosedUntil4Failures() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .build();
            
            for (int i = 0; i < 4; i++) {
                config.recordFailure();
                assertEquals(CircuitState.CLOSED, config.getCircuitState());
                assertTrue(config.getIsAvailable());
            }
            
            assertEquals(4, config.getFailureCount());
        }
        
        @Test
        @DisplayName("Deve abrir circuit breaker após 5 falhas")
        void shouldOpenCircuitAfter5Failures() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .build();
            
            // Primeira a quarta falha - circuit continua fechado
            for (int i = 0; i < 4; i++) {
                config.recordFailure();
                assertEquals(CircuitState.CLOSED, config.getCircuitState());
            }
            
            // Quinta falha - deve abrir circuit
            LocalDateTime beforeOpen = LocalDateTime.now();
            config.recordFailure();
            LocalDateTime afterOpen = LocalDateTime.now();
            
            assertEquals(5, config.getFailureCount());
            assertEquals(CircuitState.OPEN, config.getCircuitState());
            assertFalse(config.getIsAvailable());
            assertNotNull(config.getCircuitOpenUntil());
            
            // Circuit deve ficar aberto por 5 minutos
            assertTrue(config.getCircuitOpenUntil().isAfter(beforeOpen.plusMinutes(4)));
            assertTrue(config.getCircuitOpenUntil().isBefore(afterOpen.plusMinutes(6)));
        }
        
        @Test
        @DisplayName("Deve resetar falhas ao registrar sucesso")
        void shouldResetFailuresOnSuccess() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .failureCount(3)
                .lastFailure(LocalDateTime.now())
                .build();
            
            config.recordSuccess();
            
            assertEquals(0, config.getFailureCount());
            assertNull(config.getLastFailure());
            assertEquals(CircuitState.CLOSED, config.getCircuitState());
            assertTrue(config.getIsAvailable());
            assertNull(config.getCircuitOpenUntil());
        }
        
        @Test
        @DisplayName("Deve fechar circuit breaker ao registrar sucesso")
        void shouldCloseCircuitOnSuccess() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .failureCount(5)
                .circuitState(CircuitState.OPEN)
                .isAvailable(false)
                .circuitOpenUntil(LocalDateTime.now().plusMinutes(5))
                .build();
            
            config.recordSuccess();
            
            assertEquals(CircuitState.CLOSED, config.getCircuitState());
            assertTrue(config.getIsAvailable());
            assertNull(config.getCircuitOpenUntil());
        }
        
        @Test
        @DisplayName("Deve identificar quando circuit deve reabrir")
        void shouldIdentifyWhenCircuitShouldReopen() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .circuitState(CircuitState.OPEN)
                .circuitOpenUntil(LocalDateTime.now().minusMinutes(1)) // Expirou há 1 minuto
                .build();
            
            boolean shouldReopen = config.shouldReopenCircuit();
            
            assertTrue(shouldReopen);
            assertEquals(CircuitState.HALF_OPEN, config.getCircuitState());
        }
        
        @Test
        @DisplayName("Não deve reabrir circuit antes do tempo")
        void shouldNotReopenCircuitBeforeTime() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .circuitState(CircuitState.OPEN)
                .circuitOpenUntil(LocalDateTime.now().plusMinutes(3)) // Ainda faltam 3 minutos
                .build();
            
            boolean shouldReopen = config.shouldReopenCircuit();
            
            assertFalse(shouldReopen);
            assertEquals(CircuitState.OPEN, config.getCircuitState());
        }
    }
    
    // ========== TESTES DE BACKOFF ==========
    
    @Nested
    @DisplayName("Backoff Calculation")
    class BackoffCalculationTests {
        
        @Test
        @DisplayName("Deve calcular backoff exponencial corretamente")
        void shouldCalculateExponentialBackoff() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .initialBackoffMs(1000)      // 1 segundo
                .backoffMultiplier(2.0)      // 2x
                .maxBackoffMs(300000)        // 5 minutos max
                .build();
            
            assertEquals(1000, config.calculateBackoffDelay(1));   // 1s
            assertEquals(2000, config.calculateBackoffDelay(2));   // 2s
            assertEquals(4000, config.calculateBackoffDelay(3));   // 4s
            assertEquals(8000, config.calculateBackoffDelay(4));   // 8s
            assertEquals(16000, config.calculateBackoffDelay(5));  // 16s
            assertEquals(32000, config.calculateBackoffDelay(6));  // 32s
        }
        
        @Test
        @DisplayName("Deve respeitar backoff máximo")
        void shouldRespectMaxBackoff() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .initialBackoffMs(1000)
                .backoffMultiplier(2.0)
                .maxBackoffMs(10000)  // Máx 10 segundos
                .build();
            
            // Tentativa 10: seria 512 segundos, mas deve retornar máximo de 10s
            assertEquals(10000, config.calculateBackoffDelay(10));
            
            // Tentativa 20: ainda deve retornar máximo
            assertEquals(10000, config.calculateBackoffDelay(20));
        }
    }
    
    // ========== TESTES DE BUILDER ==========
    
    @Nested
    @DisplayName("Builder Patterns")
    class BuilderTests {
        
        @Test
        @DisplayName("Deve criar instituição com valores padrão")
        void shouldCreateWithDefaults() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .institutionName("Banco Teste")
                .withDefaults()
                .build();
            
            assertEquals(300, config.getGlobalTps());
            assertEquals(2500, config.getHighFreqTpm());
            assertEquals(2000, config.getMediumHighFreqTpm());
            assertEquals(1500, config.getMediumFreqTpm());
            assertEquals(1000, config.getLowFreqTpm());
            assertEquals(0L, config.getActiveConsentsCount());
            assertTrue(config.getIsAvailable());
            assertEquals(0, config.getFailureCount());
            assertEquals(CircuitState.CLOSED, config.getCircuitState());
            assertEquals("v2", config.getApiVersion());
            assertTrue(config.getRequiresMtls());
        }
        
        @Test
        @DisplayName("Deve criar instituição customizada")
        void shouldCreateCustomInstitution() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("341")
                .institutionName("Banco Itaú")
                .cnpj("60701190000104")
                .globalTps(600)
                .highFreqTpm(8000)
                .baseUrl("https://api.itau.com.br")
                .build();
            
            assertEquals("341", config.getInstitutionId());
            assertEquals("Banco Itaú", config.getInstitutionName());
            assertEquals("60701190000104", config.getCnpj());
            assertEquals(600, config.getGlobalTps());
            assertEquals(8000, config.getHighFreqTpm());
        }
    }
    
    // ========== TESTES DE AUDITORIA ==========
    
    @Nested
    @DisplayName("Audit Fields")
    class AuditFieldsTests {
        
        @Test
        @DisplayName("Deve definir createdAt e updatedAt automaticamente")
        void shouldSetTimestampsAutomatically() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .institutionName("Banco Teste")
                .build();
            
            config.onCreate();
            
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);
            
            assertNotNull(config.getCreatedAt());
            assertNotNull(config.getUpdatedAt());
            assertTrue(config.getCreatedAt().isAfter(before));
            assertTrue(config.getCreatedAt().isBefore(after));
        }
        
        @Test
        @DisplayName("Deve atualizar updatedAt no onUpdate")
        void shouldUpdateTimestampOnUpdate() throws InterruptedException {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            
            LocalDateTime originalUpdatedAt = config.getUpdatedAt();
            
            // Aguarda 1ms para garantir diferença
            Thread.sleep(1);
            
            config.onUpdate();
            
            assertNotEquals(originalUpdatedAt, config.getUpdatedAt());
            assertTrue(config.getUpdatedAt().isAfter(originalUpdatedAt));
        }
    }
    
    // ========== TESTES DE VALIDAÇÃO ==========
    
    @Nested
    @DisplayName("Validation Constraints")
    class ValidationTests {
        
        @Test
        @DisplayName("Deve aceitar valores válidos de TPS")
        void shouldAcceptValidTps() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .globalTps(300)  // Mínimo
                .build();
            
            assertNotNull(config);
            
            config.setGlobalTps(10000);  // Máximo
            assertEquals(10000, config.getGlobalTps());
        }
        
        @Test
        @DisplayName("Deve aceitar valores válidos de backoff multiplier")
        void shouldAcceptValidBackoffMultiplier() {
            InstitutionConfig config = InstitutionConfig.builder()
                .institutionId("001")
                .backoffMultiplier(1.0)  // Mínimo
                .build();
            
            assertNotNull(config);
            
            config.setBackoffMultiplier(5.0);  // Máximo
            assertEquals(5.0, config.getBackoffMultiplier());
        }
    }
}
