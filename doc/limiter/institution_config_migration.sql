-- Migration V1: Criar tabela institution_config
-- Open Finance Brasil - Sistema Receptor
-- Data: 2025-01-30

-- ========== TABELA PRINCIPAL ==========

CREATE TABLE institution_config (
    -- Identificação
    institution_id VARCHAR(100) PRIMARY KEY,
    institution_name VARCHAR(255) NOT NULL,
    cnpj VARCHAR(14) UNIQUE,
    
    -- Limites Regulatórios (Open Finance Brasil)
    global_tps INTEGER NOT NULL DEFAULT 300 
        CHECK (global_tps >= 300 AND global_tps <= 10000),
    high_freq_tpm INTEGER NOT NULL DEFAULT 2500 
        CHECK (high_freq_tpm >= 2500),
    medium_high_freq_tpm INTEGER NOT NULL DEFAULT 2000 
        CHECK (medium_high_freq_tpm >= 2000),
    medium_freq_tpm INTEGER NOT NULL DEFAULT 1500 
        CHECK (medium_freq_tpm >= 1500),
    low_freq_tpm INTEGER NOT NULL DEFAULT 1000 
        CHECK (low_freq_tpm >= 1000),
    
    -- Consentimentos ativos
    active_consents_count BIGINT DEFAULT 0 
        CHECK (active_consents_count >= 0),
    
    -- Circuit Breaker
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    failure_count INTEGER DEFAULT 0 
        CHECK (failure_count >= 0),
    last_failure TIMESTAMP,
    circuit_open_until TIMESTAMP,
    circuit_state VARCHAR(20) DEFAULT 'CLOSED' 
        CHECK (circuit_state IN ('CLOSED', 'OPEN', 'HALF_OPEN')),
    
    -- Configurações de Retry
    initial_backoff_ms INTEGER DEFAULT 1000 
        CHECK (initial_backoff_ms BETWEEN 100 AND 60000),
    backoff_multiplier DECIMAL(3,1) DEFAULT 2.0 
        CHECK (backoff_multiplier BETWEEN 1.0 AND 5.0),
    max_backoff_ms INTEGER DEFAULT 300000 
        CHECK (max_backoff_ms BETWEEN 1000 AND 3600000),
    max_retry_attempts INTEGER DEFAULT 3 
        CHECK (max_retry_attempts BETWEEN 1 AND 10),
    
    -- Timeouts
    connection_timeout_seconds INTEGER DEFAULT 10 
        CHECK (connection_timeout_seconds BETWEEN 1 AND 60),
    read_timeout_seconds INTEGER DEFAULT 30 
        CHECK (read_timeout_seconds BETWEEN 1 AND 300),
    
    -- Metadata da Instituição
    base_url VARCHAR(500),
    api_version VARCHAR(20) DEFAULT 'v2',
    requires_mtls BOOLEAN DEFAULT TRUE,
    notes TEXT,
    
    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- ========== ÍNDICES ==========

CREATE INDEX idx_institution_available 
    ON institution_config (is_available);

CREATE INDEX idx_institution_updated 
    ON institution_config (updated_at);

CREATE INDEX idx_institution_circuit_state 
    ON institution_config (circuit_state);

CREATE INDEX idx_institution_cnpj 
    ON institution_config (cnpj) 
    WHERE cnpj IS NOT NULL;

CREATE INDEX idx_institution_failure_count 
    ON institution_config (failure_count) 
    WHERE failure_count > 0;

-- ========== TRIGGER PARA UPDATED_AT ==========

CREATE OR REPLACE FUNCTION update_institution_config_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_institution_config_updated_at
    BEFORE UPDATE ON institution_config
    FOR EACH ROW
    EXECUTE FUNCTION update_institution_config_updated_at();

-- ========== COMENTÁRIOS ==========

COMMENT ON TABLE institution_config IS 
    'Configurações de rate limiting e circuit breaker por instituição participante do Open Finance Brasil';

COMMENT ON COLUMN institution_config.institution_id IS 
    'Código COMPE da instituição (3 dígitos)';

COMMENT ON COLUMN institution_config.global_tps IS 
    'Limite global de TPS (Transactions Per Second) - mínimo regulatório: 300 TPS';

COMMENT ON COLUMN institution_config.high_freq_tpm IS 
    'Limite de TPM (Transactions Per Minute) para endpoints de alta frequência - varia baseado em consentimentos ativos';

COMMENT ON COLUMN institution_config.circuit_state IS 
    'Estado do circuit breaker: CLOSED (normal), OPEN (bloqueado), HALF_OPEN (em teste)';

-- ========== DADOS INICIAIS (Principais bancos brasileiros) ==========

INSERT INTO institution_config (
    institution_id, 
    institution_name, 
    cnpj,
    global_tps,
    high_freq_tpm,
    base_url,
    notes,
    created_by
) VALUES
    -- Grandes Bancos Públicos
    ('001', 'Banco do Brasil S.A.', '00000000000191', 
     500, 5000, 'https://api.bb.com.br/open-banking', 
     'Maior banco público do Brasil', 'SYSTEM'),
    
    ('104', 'Caixa Econômica Federal', '00360305000104', 
     500, 5000, 'https://api.caixa.gov.br/open-banking', 
     'Banco público federal', 'SYSTEM'),
    
    -- Grandes Bancos Privados
    ('237', 'Banco Bradesco S.A.', '60746948000112', 
     500, 5000, 'https://api.bradesco.com.br/open-banking', 
     'Segundo maior banco privado', 'SYSTEM'),
    
    ('341', 'Banco Itaú Unibanco S.A.', '60701190000104', 
     600, 8000, 'https://api.itau.com.br/open-banking', 
     'Maior banco privado do Brasil', 'SYSTEM'),
    
    ('033', 'Banco Santander Brasil S.A.', '90400888000142', 
     450, 5000, 'https://api.santander.com.br/open-banking', 
     'Banco de origem espanhola', 'SYSTEM'),
    
    -- Bancos Digitais
    ('077', 'Banco Inter S.A.', '00416968000101', 
     350, 3000, 'https://api.bancointer.com.br/open-banking', 
     'Banco digital completo', 'SYSTEM'),
    
    ('260', 'Nu Pagamentos S.A. (Nubank)', '18236120000158', 
     400, 4000, 'https://api.nubank.com.br/open-banking', 
     'Maior banco digital da América Latina', 'SYSTEM'),
    
    ('290', 'PagSeguro Internet S.A.', '08561701000101', 
     350, 3000, 'https://api.pagseguro.uol.com.br/open-banking', 
     'Fintech de pagamentos', 'SYSTEM'),
    
    ('323', 'Mercado Pago', '10573521000191', 
     350, 3000, 'https://api.mercadopago.com.br/open-banking', 
     'Fintech do grupo Mercado Livre', 'SYSTEM'),
    
    ('735', 'Banco Neon S.A.', '20855875000148', 
     300, 2500, 'https://api.banconeon.com.br/open-banking', 
     'Banco digital', 'SYSTEM'),
    
    -- Bancos Regionais
    ('041', 'Banrisul', '92702067000196', 
     350, 3000, 'https://api.banrisul.com.br/open-banking', 
     'Banco do Estado do Rio Grande do Sul', 'SYSTEM'),
    
    ('212', 'Banco Original S.A.', '92894922000135', 
     350, 3000, 'https://api.original.com.br/open-banking', 
     'Banco digital do grupo JBS', 'SYSTEM'),
    
    -- Fintechs e Outros
    ('336', 'Banco C6 S.A.', '31872495000172', 
     350, 3000, 'https://api.c6bank.com.br/open-banking', 
     'Banco digital', 'SYSTEM'),
    
    ('197', 'Stone Pagamentos S.A.', '16501555000157', 
     350, 3000, 'https://api.stone.com.br/open-banking', 
     'Fintech de pagamentos e crédito', 'SYSTEM');

-- ========== VIEW DE MONITORAMENTO ==========

CREATE OR REPLACE VIEW v_institution_health AS
SELECT 
    institution_id,
    institution_name,
    is_available,
    circuit_state,
    failure_count,
    global_tps,
    high_freq_tpm,
    active_consents_count,
    CASE 
        WHEN circuit_open_until IS NOT NULL AND circuit_open_until > NOW() 
        THEN EXTRACT(EPOCH FROM (circuit_open_until - NOW()))::INTEGER
        ELSE 0 
    END as seconds_until_reopen,
    last_failure,
    updated_at,
    CASE 
        WHEN NOT is_available THEN 'CRITICAL'
        WHEN failure_count >= 3 THEN 'WARNING'
        WHEN failure_count > 0 THEN 'DEGRADED'
        ELSE 'HEALTHY'
    END as health_status
FROM institution_config
ORDER BY 
    CASE circuit_state 
        WHEN 'OPEN' THEN 1 
        WHEN 'HALF_OPEN' THEN 2 
        ELSE 3 
    END,
    failure_count DESC,
    institution_name;

COMMENT ON VIEW v_institution_health IS 
    'View para monitorar saúde das instituições - ordenada por criticidade';

-- ========== PROCEDURES ÚTEIS ==========

-- Procedure para simular falha (útil para testes)
CREATE OR REPLACE PROCEDURE simulate_failure(p_institution_id VARCHAR)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE institution_config
    SET failure_count = failure_count + 1,
        last_failure = NOW(),
        updated_at = NOW()
    WHERE institution_id = p_institution_id;
    
    RAISE NOTICE 'Falha simulada para instituição: %', p_institution_id;
END;
$$;

-- Procedure para resetar todas as falhas (útil para manutenção)
CREATE OR REPLACE PROCEDURE reset_all_failures()
LANGUAGE plpgsql
AS $$
DECLARE
    v_count INTEGER;
BEGIN
    UPDATE institution_config
    SET failure_count = 0,
        last_failure = NULL,
        circuit_state = 'CLOSED',
        is_available = TRUE,
        circuit_open_until = NULL,
        updated_at = NOW()
    WHERE failure_count > 0;
    
    GET DIAGNOSTICS v_count = ROW_COUNT;
    RAISE NOTICE 'Resetadas % instituições', v_count;
END;
$$;

-- ========== QUERIES DE VALIDAÇÃO ==========

-- Validar que todas as instituições têm limites corretos
DO $$
DECLARE
    v_invalid_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_invalid_count
    FROM institution_config
    WHERE global_tps < 300 
       OR high_freq_tpm < 2500
       OR medium_high_freq_tpm < 2000
       OR medium_freq_tpm < 1500
       OR low_freq_tpm < 1000;
    
    IF v_invalid_count > 0 THEN
        RAISE EXCEPTION 'Existem % instituições com limites inválidos!', v_invalid_count;
    END IF;
    
    RAISE NOTICE 'Validação OK: Todas as instituições têm limites regulatórios corretos';
END;
$$;

-- ========== GRANTS (ajustar conforme seu ambiente) ==========

-- GRANT SELECT, INSERT, UPDATE, DELETE ON institution_config TO openfinance_app;
-- GRANT SELECT ON v_institution_health TO openfinance_readonly;

-- ========== FIM DA MIGRATION ==========
