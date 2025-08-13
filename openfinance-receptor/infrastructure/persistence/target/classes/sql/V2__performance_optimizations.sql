-- V2__performance_optimizations.sql
-- Advanced performance optimizations for parallel processing

-- Add new columns to processing_jobs for enhanced monitoring and distributed processing
ALTER TABLE processing_jobs ADD COLUMN IF NOT EXISTS execution_node VARCHAR(50);
ALTER TABLE processing_jobs ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 0;
ALTER TABLE processing_jobs ADD COLUMN IF NOT EXISTS estimated_duration_ms BIGINT;
ALTER TABLE processing_jobs ADD COLUMN IF NOT EXISTS actual_duration_ms BIGINT;

-- Enhanced indexes for processing_jobs with better selectivity
DROP INDEX IF EXISTS idx_pending_jobs;
CREATE INDEX CONCURRENTLY idx_processing_jobs_status_priority_created 
    ON processing_jobs(status, priority DESC, created_at ASC) 
    WHERE status IN ('PENDING', 'FAILED');

CREATE INDEX CONCURRENTLY idx_processing_jobs_execution_node 
    ON processing_jobs(execution_node, status, updated_at);

CREATE INDEX CONCURRENTLY idx_processing_jobs_performance 
    ON processing_jobs(status, actual_duration_ms, created_at) 
    WHERE actual_duration_ms IS NOT NULL;

-- Partial index for retry logic optimization
CREATE INDEX CONCURRENTLY idx_processing_jobs_retry 
    ON processing_jobs(status, retry_count, updated_at) 
    WHERE status = 'FAILED' AND retry_count < 3;

-- Create balances table for account balance management
CREATE TABLE IF NOT EXISTS balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    available_amount DECIMAL(19,4),
    available_amount_currency CHAR(3) DEFAULT 'BRL',
    blocked_amount DECIMAL(19,4),
    blocked_amount_currency CHAR(3) DEFAULT 'BRL',
    automatically_invested_amount DECIMAL(19,4),
    automatically_invested_amount_currency CHAR(3) DEFAULT 'BRL',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version BIGINT DEFAULT 0,
    
    CONSTRAINT uk_balances_account_id UNIQUE (account_id)
);

-- Performance indexes for balances
CREATE INDEX CONCURRENTLY idx_balances_updated_at ON balances(updated_at DESC);
CREATE INDEX CONCURRENTLY idx_balances_currency ON balances(available_amount_currency);

-- Add missing columns to accounts table
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS brand_name VARCHAR(100);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS company_cnpj CHAR(14);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS subtype VARCHAR(30);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS check_digit VARCHAR(2);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS agency_number VARCHAR(10);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS agency_check_digit VARCHAR(2);
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Enhanced indexes for accounts with better query performance
CREATE INDEX CONCURRENTLY idx_accounts_sync_status 
    ON accounts(last_sync_at ASC NULLS FIRST, consent_id) 
    WHERE last_sync_at < NOW() - INTERVAL '1 hour' OR last_sync_at IS NULL;

CREATE INDEX CONCURRENTLY idx_accounts_type_org 
    ON accounts(type, organization_id);

CREATE INDEX CONCURRENTLY idx_accounts_company_cnpj 
    ON accounts(company_cnpj) WHERE company_cnpj IS NOT NULL;

-- Add performance columns to consents
ALTER TABLE consents ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Composite indexes for complex queries
CREATE INDEX CONCURRENTLY idx_consents_status_org_created 
    ON consents(status, organization_id, created_at DESC);

CREATE INDEX CONCURRENTLY idx_consents_expiration_status 
    ON consents(expiration_date_time ASC, status) 
    WHERE expiration_date_time IS NOT NULL 
    AND status NOT IN ('EXPIRED', 'REJECTED');

-- Create materialized view for dashboard queries
CREATE MATERIALIZED VIEW IF NOT EXISTS consent_statistics AS
SELECT 
    DATE_TRUNC('day', created_at) as date,
    status,
    organization_id,
    COUNT(*) as count,
    COUNT(*) FILTER (WHERE expiration_date_time < NOW()) as expired_count
FROM consents 
WHERE created_at >= NOW() - INTERVAL '30 days'
GROUP BY DATE_TRUNC('day', created_at), status, organization_id;

-- Index for the materialized view
CREATE UNIQUE INDEX IF NOT EXISTS idx_consent_statistics 
    ON consent_statistics(date, status, organization_id);

-- Create function for efficient batch job fetching
CREATE OR REPLACE FUNCTION fetch_next_job_batch(
    batch_size INTEGER DEFAULT 50,
    node_id TEXT DEFAULT NULL
) RETURNS TABLE (
    job_id BIGINT,
    consent_id UUID,
    organization_id TEXT,
    retry_count INTEGER,
    created_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    WITH next_jobs AS (
        SELECT pj.id
        FROM processing_jobs pj
        WHERE pj.status = 'PENDING'
          AND (pj.execution_node IS NULL OR pj.execution_node != node_id)
        ORDER BY pj.priority DESC, pj.created_at ASC
        LIMIT batch_size
        FOR UPDATE SKIP LOCKED
    )
    UPDATE processing_jobs
    SET status = 'PROCESSING',
        execution_node = node_id,
        updated_at = NOW()
    WHERE id IN (SELECT id FROM next_jobs)
    RETURNING 
        processing_jobs.id as job_id,
        processing_jobs.consent_id,
        processing_jobs.organization_id,
        processing_jobs.retry_count,
        processing_jobs.created_at;
END;
$$ LANGUAGE plpgsql;

-- Create function for batch status updates
CREATE OR REPLACE FUNCTION update_job_statuses_batch(
    job_ids BIGINT[],
    new_status TEXT,
    error_message TEXT DEFAULT NULL,
    duration_ms BIGINT DEFAULT NULL
) RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE processing_jobs 
    SET status = new_status,
        error_details = COALESCE(error_message, error_details),
        actual_duration_ms = COALESCE(duration_ms, actual_duration_ms),
        updated_at = NOW(),
        retry_count = CASE 
            WHEN new_status = 'FAILED' THEN retry_count + 1 
            ELSE retry_count 
        END
    WHERE id = ANY(job_ids);
    
    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql;

-- Optimize PostgreSQL settings for high concurrency (commented for manual execution)
-- These should be set in postgresql.conf for production

/*
-- Connection and memory settings
max_connections = 200
shared_buffers = '256MB'
effective_cache_size = '1GB'
work_mem = '4MB'
maintenance_work_mem = '64MB'

-- WAL and checkpoint settings
wal_buffers = '16MB'
checkpoint_segments = 32
checkpoint_completion_target = 0.9
wal_writer_delay = '200ms'

-- Query planner settings
random_page_cost = 1.1
effective_io_concurrency = 200
max_worker_processes = 8
max_parallel_workers_per_gather = 4
max_parallel_workers = 8
parallel_tuple_cost = 0.1
parallel_setup_cost = 1000

-- Lock and vacuum settings
deadlock_timeout = '1s'
lock_timeout = '30s'
autovacuum_max_workers = 3
autovacuum_naptime = '20s'
*/

-- Add comments for monitoring queries
COMMENT ON TABLE processing_jobs IS 'Queue table for background job processing with Virtual Threads support';
COMMENT ON COLUMN processing_jobs.execution_node IS 'Node ID for distributed processing tracking';
COMMENT ON COLUMN processing_jobs.priority IS 'Job priority for queue ordering (higher = more important)';
COMMENT ON FUNCTION fetch_next_job_batch IS 'Efficient batch job fetching with SKIP LOCKED for concurrency';