ALTER TABLE notifications
    ADD COLUMN available_at TIMESTAMP NULL;

UPDATE notifications SET available_at = created_at WHERE available_at IS NULL;

ALTER TABLE notifications
    MODIFY COLUMN available_at TIMESTAMP NOT NULL;

ALTER TABLE notification_recipients
    ADD COLUMN available_at TIMESTAMP NULL;

UPDATE notification_recipients SET available_at = created_at WHERE available_at IS NULL;

ALTER TABLE notification_recipients
    MODIFY COLUMN available_at TIMESTAMP NOT NULL;

CREATE INDEX idx_notifications_recipient_available
    ON notifications(recipient_id, available_at, created_at);

CREATE INDEX idx_notification_recipients_available
    ON notification_recipients(recipient_user_id, is_deleted, available_at, created_at);

CREATE TABLE notification_email_outbox (
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

CREATE INDEX idx_notification_email_outbox_due
    ON notification_email_outbox(delivered_at, available_at);

CREATE INDEX idx_notification_email_outbox_digest
    ON notification_email_outbox(recipient_user_id, available_at, digest_frequency, delivered_at);
