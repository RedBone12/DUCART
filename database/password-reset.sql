-- MySQL 8.4: additive migration for password reset tokens and shared request limits.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    user_id BIGINT NOT NULL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used BIT NOT NULL,
    CONSTRAINT uk_reset_token_hash UNIQUE (token_hash)
);
CREATE TABLE IF NOT EXISTS password_reset_limits (
    bucket_key VARCHAR(64) NOT NULL PRIMARY KEY,
    window_start DATETIME(6) NOT NULL,
    attempts INT NOT NULL,
    INDEX idx_reset_limit_window (window_start)
);
