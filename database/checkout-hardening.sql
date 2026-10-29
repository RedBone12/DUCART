-- MySQL 8.4: run once against an existing database BEFORE starting the new backend.
-- Stop writers and back up the database first. DDL commits implicitly in MySQL.
-- Fresh databases are created with these definitions by Hibernate.
ALTER TABLE checkout
    MODIFY COLUMN subtotal DECIMAL(19, 2),
    MODIFY COLUMN shipping DECIMAL(19, 2),
    MODIFY COLUMN total DECIMAL(19, 2),
    ADD COLUMN idempotency_key VARCHAR(64) NULL,
    ADD COLUMN request_fingerprint VARCHAR(64) NULL,
    ADD CONSTRAINT uk_checkout_user_request UNIQUE (`user`, idempotency_key);

ALTER TABLE cart ADD COLUMN product_id BIGINT NULL;
