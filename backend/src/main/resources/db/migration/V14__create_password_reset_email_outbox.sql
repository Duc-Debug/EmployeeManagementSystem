CREATE TABLE password_reset_email_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL,
    reset_token VARCHAR(255) NOT NULL,
    validity_minutes BIGINT NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    last_error VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reset_email_outbox_pending
    ON password_reset_email_outbox(delivered_at, available_at, created_at);
