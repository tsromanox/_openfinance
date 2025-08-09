-- Criar schema e tabelas básicas para PostgreSQL (desenvolvimento)
CREATE SCHEMA IF NOT EXISTS openfinance;

-- Tabela de contas
CREATE TABLE IF NOT EXISTS openfinance.accounts (
    id VARCHAR(100) PRIMARY KEY,
    account_id VARCHAR(100) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    consent_id VARCHAR(100) NOT NULL,
    institution_id VARCHAR(50) NOT NULL,
    brand_name VARCHAR(80),
    company_cnpj VARCHAR(14),
    account_type VARCHAR(50),
    compe_code VARCHAR(3),
    branch_code VARCHAR(4),
    number VARCHAR(20),
    check_digit VARCHAR(1),
    status VARCHAR(20),
    last_updated TIMESTAMP,
    partition_key VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(account_id, institution_id)
);

-- Índices
CREATE INDEX idx_accounts_client_id ON openfinance.accounts(client_id);
CREATE INDEX idx_accounts_consent_id ON openfinance.accounts(consent_id);
CREATE INDEX idx_accounts_institution_id ON openfinance.accounts(institution_id);
CREATE INDEX idx_accounts_last_updated ON openfinance.accounts(last_updated);

-- Tabela de saldos
CREATE TABLE IF NOT EXISTS openfinance.account_balances (
    id SERIAL PRIMARY KEY,
    account_id VARCHAR(100) NOT NULL,
    available_amount DECIMAL(20,4),
    available_amount_currency VARCHAR(3),
    blocked_amount DECIMAL(20,4),
    blocked_amount_currency VARCHAR(3),
    automatically_invested_amount DECIMAL(20,4),
    automatically_invested_amount_currency VARCHAR(3),
    update_date_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES openfinance.accounts(id)
);

-- Tabela de limites
CREATE TABLE IF NOT EXISTS openfinance.account_limits (
    account_id VARCHAR(100) PRIMARY KEY,
    overdraft_contracted_limit DECIMAL(20,4),
    overdraft_contracted_limit_currency VARCHAR(3),
    overdraft_used_limit DECIMAL(20,4),
    overdraft_used_limit_currency VARCHAR(3),
    unarranged_overdraft_amount DECIMAL(20,4),
    unarranged_overdraft_amount_currency VARCHAR(3),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES openfinance.accounts(id)
);

-- Criar usuário para aplicação
CREATE USER openfinance_app WITH PASSWORD 'app123';
GRANT ALL PRIVILEGES ON SCHEMA openfinance TO openfinance_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA openfinance TO openfinance_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA openfinance TO openfinance_app;