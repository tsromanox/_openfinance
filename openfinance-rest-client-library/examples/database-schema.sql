-- Database schema for event consumption
-- Compatible with PostgreSQL 13+

-- Create events table for database-based event consumption
CREATE TABLE IF NOT EXISTS api_events (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Event metadata
    event_type VARCHAR(100) NOT NULL,
    source VARCHAR(20) DEFAULT 'DATABASE' CHECK (source IN ('DATABASE', 'KAFKA')),
    version VARCHAR(20) DEFAULT '1.0',
    
    -- Event payload (JSON)
    payload JSONB NOT NULL,
    
    -- Correlation and tracking
    correlation_id UUID,
    partition_key VARCHAR(255),
    
    -- Processing control
    processed BOOLEAN DEFAULT FALSE,
    retry_count INTEGER DEFAULT 0,
    max_retry_attempts INTEGER DEFAULT 3,
    
    -- Priority and routing
    priority VARCHAR(20) DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    target_endpoint VARCHAR(500),
    http_method VARCHAR(10) DEFAULT 'POST',
    
    -- Additional metadata
    headers JSONB,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_api_events_processed 
    ON api_events (processed, retry_count, priority, created_at) 
    WHERE processed = FALSE;

CREATE INDEX IF NOT EXISTS idx_api_events_correlation 
    ON api_events (correlation_id);

CREATE INDEX IF NOT EXISTS idx_api_events_type 
    ON api_events (event_type);

CREATE INDEX IF NOT EXISTS idx_api_events_priority 
    ON api_events (priority, created_at) 
    WHERE processed = FALSE;

-- Create partial index for unprocessed events
CREATE INDEX IF NOT EXISTS idx_api_events_unprocessed 
    ON api_events (created_at, priority) 
    WHERE processed = FALSE AND retry_count < max_retry_attempts;

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to automatically update updated_at
CREATE TRIGGER update_api_events_updated_at 
    BEFORE UPDATE ON api_events 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Example event data
INSERT INTO api_events (
    event_type,
    payload,
    correlation_id,
    target_endpoint,
    http_method,
    priority
) VALUES 
(
    'ACCOUNT_UPDATE',
    '{"accountId": "12345678901234567890", "customerId": "user123", "action": "UPDATE_BALANCE"}',
    gen_random_uuid(),
    '/accounts/12345678901234567890/sync',
    'POST',
    'HIGH'
),
(
    'CONSENT_CREATED',
    '{"consentId": "consent-456", "customerId": "user123", "permissions": ["ACCOUNTS_READ", "ACCOUNTS_BALANCES_READ"]}',
    gen_random_uuid(),
    '/consents/consent-456/process',
    'POST',
    'NORMAL'
),
(
    'ACCOUNT_BATCH_UPDATE',
    '{"accountIds": ["acc1", "acc2", "acc3"], "batchId": "batch-789"}',
    gen_random_uuid(),
    '/accounts/batch/sync',
    'POST',
    'CRITICAL'
);

-- View for monitoring events
CREATE OR REPLACE VIEW api_events_monitor AS
SELECT 
    event_type,
    COUNT(*) as total_events,
    COUNT(*) FILTER (WHERE processed = true) as processed_events,
    COUNT(*) FILTER (WHERE processed = false) as pending_events,
    COUNT(*) FILTER (WHERE retry_count >= max_retry_attempts) as failed_events,
    AVG(EXTRACT(EPOCH FROM (processed_at - created_at))) as avg_processing_time_seconds,
    MIN(created_at) as oldest_unprocessed_event,
    MAX(created_at) as latest_event
FROM api_events
GROUP BY event_type
ORDER BY total_events DESC;

-- Function to cleanup old processed events
CREATE OR REPLACE FUNCTION cleanup_old_processed_events(retention_days INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM api_events 
    WHERE processed = true 
      AND processed_at < NOW() - INTERVAL '1 day' * retention_days;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Example usage:
-- SELECT cleanup_old_processed_events(30); -- Clean up events older than 30 days