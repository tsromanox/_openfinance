-- V1__create_base_tables.sql

-- Sequence para jobs
CREATE SEQUENCE job_seq START WITH 1 INCREMENT BY 100;

-- Tabela de jobs de processamento
CREATE TABLE processing_jobs (
    id BIGINT PRIMARY KEY DEFAULT nextval('job_seq'),
    consent_id UUID NOT NULL,
    organization_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    error_details TEXT,
    version BIGINT DEFAULT 0
);

-- Índice parcial para performance na fila
CREATE INDEX idx_pending_jobs ON processing_jobs(created_at)
WHERE status = 'PENDING';

-- Tabela de consentimentos
CREATE TABLE consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consent_id VARCHAR(256) UNIQUE NOT NULL,
    organization_id VARCHAR(100) NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    permissions TEXT[] NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expiration_date_time TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_consents_org ON consents(organization_id);
CREATE INDEX idx_consents_status ON consents(status);

-- Tabela de contas
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id VARCHAR(100) UNIQUE NOT NULL,
    consent_id UUID REFERENCES consents(id),
    organization_id VARCHAR(100) NOT NULL,
    number VARCHAR(20) NOT NULL,
    type VARCHAR(30) NOT NULL,
    last_sync_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_accounts_consent ON accounts(consent_id);