CREATE TABLE IF NOT EXISTS sms_dispatches (
    id VARCHAR(36) PRIMARY KEY,
    target_mobile_endpoint VARCHAR(32) NOT NULL,
    sms_message_text VARCHAR(160) NOT NULL,
    signaling_protocol VARCHAR(32) NOT NULL,
    provider_message_id VARCHAR(128) NOT NULL,
    gateway_status VARCHAR(24) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sms_dispatches_submitted_at
    ON sms_dispatches (submitted_at);

CREATE INDEX IF NOT EXISTS idx_sms_dispatches_gateway_status
    ON sms_dispatches (gateway_status);
