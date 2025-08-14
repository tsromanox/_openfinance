-- Accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    account_id VARCHAR(100) UNIQUE NOT NULL,
    consent_id UUID NOT NULL,
    organization_id VARCHAR(100) NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    compe_code VARCHAR(3),
    branch_code VARCHAR(4),
    number VARCHAR(20) NOT NULL,
    check_digit VARCHAR(1),
    type VARCHAR(30) NOT NULL,
    subtype VARCHAR(30),
    currency VARCHAR(3) DEFAULT 'BRL',
    status VARCHAR(30) DEFAULT 'AVAILABLE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_sync_at TIMESTAMP WITH TIME ZONE,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_accounts_consent ON accounts(consent_id);
CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_accounts_organization ON accounts(organization_id);
CREATE INDEX idx_accounts_sync ON accounts(last_sync_at) WHERE status = 'AVAILABLE';

-- Account Balances table
CREATE TABLE IF NOT EXISTS account_balances (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id),
    available_amount DECIMAL(19,4),
    blocked_amount DECIMAL(19,4),
    automatically_invested_amount DECIMAL(19,4),
    overdraft_contracted_limit DECIMAL(19,4),
    overdraft_used_limit DECIMAL(19,4),
    currency VARCHAR(3) DEFAULT 'BRL',
    update_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_balances_account ON account_balances(account_id);
CREATE INDEX idx_balances_update ON account_balances(update_date_time DESC);

-- Account Transactions table
CREATE TABLE IF NOT EXISTS account_transactions (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(100) UNIQUE NOT NULL,
    account_id UUID NOT NULL REFERENCES accounts(id),
    type VARCHAR(50) NOT NULL,
    credit_debit_type VARCHAR(10) NOT NULL,
    transaction_name VARCHAR(200),
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'BRL',
    transaction_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    partie_cnpj_cpf VARCHAR(14),
    partie_person_type VARCHAR(20),
    partie_compe_code VARCHAR(3),
    partie_branch_code VARCHAR(4),
    partie_number VARCHAR(20),
    completed_authorised_payment_type VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_transactions_account ON account_transactions(account_id);
CREATE INDEX idx_transactions_date ON account_transactions(transaction_date_time DESC);
CREATE INDEX idx_transactions_type ON account_transactions(type);