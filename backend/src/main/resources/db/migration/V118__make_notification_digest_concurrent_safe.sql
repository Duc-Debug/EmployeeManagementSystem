-- Safe migration for V118: Make notification digest concurrent safe

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS notification_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    level VARCHAR(20) NOT NULL DEFAULT 'TRUNG_BINH',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    related_entity_type VARCHAR(100) NULL,
    related_entity_id VARCHAR(100) NULL,
    source_event_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_events_source_key UNIQUE (source_event_key)
);

CREATE TABLE IF NOT EXISTS notification_digest_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_event_id BIGINT NOT NULL,
    source_event_key VARCHAR(255) NOT NULL,
    item_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_digest_item_event_source
        UNIQUE (notification_event_id, source_event_key),
    CONSTRAINT fk_notification_digest_item_event
        FOREIGN KEY (notification_event_id) REFERENCES notification_events(id) ON DELETE CASCADE
);

DROP PROCEDURE IF EXISTS upgrade_v118;
DELIMITER $$
CREATE PROCEDURE upgrade_v118()
BEGIN
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_digest_items'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_digest_items' AND INDEX_NAME = 'idx_notification_digest_items_event'
    ) THEN
        CREATE INDEX idx_notification_digest_items_event
            ON notification_digest_items(notification_event_id, created_at);
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'digest_batch_key'
    ) THEN
        ALTER TABLE notifications ADD COLUMN digest_batch_key VARCHAR(255) NULL;
        UPDATE notifications
        SET digest_batch_key = CONCAT(recipient_id, ':', target_id, ':', available_at)
        WHERE type = 'NOTIFICATION_DIGEST';
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND INDEX_NAME = 'uk_notifications_digest_batch'
    ) THEN
        CREATE UNIQUE INDEX uk_notifications_digest_batch
            ON notifications(digest_batch_key);
    END IF;

    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_email_outbox'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_email_outbox' AND INDEX_NAME = 'uk_notification_email_digest_batch'
    ) THEN
        CREATE UNIQUE INDEX uk_notification_email_digest_batch
            ON notification_email_outbox(recipient_user_id, available_at, digest_frequency);
    END IF;
END $$
DELIMITER ;

CALL upgrade_v118();
DROP PROCEDURE IF EXISTS upgrade_v118;

SET FOREIGN_KEY_CHECKS = 1;
