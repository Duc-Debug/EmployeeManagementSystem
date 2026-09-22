-- Safe migration for V117: Add notification available_at with column and table checks

DROP PROCEDURE IF EXISTS upgrade_v117;
DELIMITER $$
CREATE PROCEDURE upgrade_v117()
BEGIN
    -- 1. Add available_at to notifications if table exists and column does not exist
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'available_at'
    ) THEN
        ALTER TABLE notifications ADD COLUMN available_at TIMESTAMP NULL;
        UPDATE notifications SET available_at = created_at WHERE available_at IS NULL;
        ALTER TABLE notifications MODIFY COLUMN available_at TIMESTAMP NOT NULL;
    END IF;

    -- 2. Add available_at to notification_recipients if table exists and column does not exist
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_recipients'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_recipients' AND COLUMN_NAME = 'available_at'
    ) THEN
        ALTER TABLE notification_recipients ADD COLUMN available_at TIMESTAMP NULL;
        UPDATE notification_recipients SET available_at = created_at WHERE available_at IS NULL;
        ALTER TABLE notification_recipients MODIFY COLUMN available_at TIMESTAMP NOT NULL;
    END IF;

    -- 3. Create index idx_notifications_recipient_available if table exists and index does not exist
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND INDEX_NAME = 'idx_notifications_recipient_available'
    ) THEN
        CREATE INDEX idx_notifications_recipient_available ON notifications(recipient_id, available_at, created_at);
    END IF;

    -- 4. Create index idx_notification_recipients_available if table exists and index does not exist
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_recipients'
    ) AND NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_recipients' AND INDEX_NAME = 'idx_notification_recipients_available'
    ) THEN
        CREATE INDEX idx_notification_recipients_available ON notification_recipients(recipient_user_id, is_deleted, available_at, created_at);
    END IF;

    -- 5. Create table notification_email_outbox if not exists
    CREATE TABLE IF NOT EXISTS notification_email_outbox (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        recipient_user_id BIGINT NOT NULL,
        recipient_email VARCHAR(255) NOT NULL,
        recipient_name VARCHAR(255),
        subject VARCHAR(500) NOT NULL,
        body TEXT NOT NULL,
        available_at TIMESTAMP NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        delivered_at TIMESTAMP NULL,
        digest_frequency VARCHAR(30) NULL
    );

    -- 6. Create indexes on notification_email_outbox if not exists
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_email_outbox' AND INDEX_NAME = 'idx_notification_email_outbox_due'
    ) THEN
        CREATE INDEX idx_notification_email_outbox_due ON notification_email_outbox(delivered_at, available_at);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_email_outbox' AND INDEX_NAME = 'idx_notification_email_outbox_digest'
    ) THEN
        CREATE INDEX idx_notification_email_outbox_digest ON notification_email_outbox(recipient_user_id, available_at, digest_frequency, delivered_at);
    END IF;
END $$
DELIMITER ;

CALL upgrade_v117();
DROP PROCEDURE IF EXISTS upgrade_v117;
