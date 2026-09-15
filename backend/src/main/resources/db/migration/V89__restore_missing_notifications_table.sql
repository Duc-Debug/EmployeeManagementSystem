-- Repair databases where V73 is recorded in Flyway history but the table was
-- removed or was never created. This is intentionally idempotent.
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    sender_id BIGINT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'TASK_MENTION',
    target_type VARCHAR(50) NOT NULL DEFAULT 'TASK',
    target_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_sender
        FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_notifications_recipient_read (recipient_id, is_read, created_at)
);
