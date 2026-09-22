-- Safe migration for V119: Unify notification channel delivery

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS notification_email_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_user_id BIGINT NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255),
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    digest_frequency VARCHAR(30) NULL
);

CREATE TABLE IF NOT EXISTS notification_email_digest_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbox_id BIGINT NOT NULL,
    source_event_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_email_digest_item_source
        UNIQUE (outbox_id, source_event_key),
    CONSTRAINT fk_notification_email_digest_item_outbox
        FOREIGN KEY (outbox_id) REFERENCES notification_email_outbox(id) ON DELETE CASCADE
);

DROP PROCEDURE IF EXISTS upgrade_v119;
DELIMITER $$
CREATE PROCEDURE upgrade_v119()
BEGIN
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_digest_items'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_digest_items' AND COLUMN_NAME = 'channel'
    ) THEN
        ALTER TABLE notification_digest_items
            ADD COLUMN channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP';
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_email_outbox'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_email_outbox' AND COLUMN_NAME = 'source_event_key'
    ) THEN
        ALTER TABLE notification_email_outbox
            ADD COLUMN source_event_key VARCHAR(255) NULL;
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_email_outbox'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_email_outbox' AND INDEX_NAME = 'uk_notification_email_immediate_source'
    ) THEN
        CREATE UNIQUE INDEX uk_notification_email_immediate_source
            ON notification_email_outbox(recipient_user_id, source_event_key);
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_email_digest_items'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_email_digest_items' AND INDEX_NAME = 'idx_notification_email_digest_items_outbox'
    ) THEN
        CREATE INDEX idx_notification_email_digest_items_outbox
            ON notification_email_digest_items(outbox_id, created_at);
    END IF;
END $$
DELIMITER ;

CALL upgrade_v119();
DROP PROCEDURE IF EXISTS upgrade_v119;

SET FOREIGN_KEY_CHECKS = 1;
