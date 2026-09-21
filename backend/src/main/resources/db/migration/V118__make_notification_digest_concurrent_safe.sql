CREATE TABLE notification_digest_items (
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

CREATE INDEX idx_notification_digest_items_event
    ON notification_digest_items(notification_event_id, created_at);

ALTER TABLE notifications ADD COLUMN digest_batch_key VARCHAR(255) NULL;

UPDATE notifications
SET digest_batch_key = CONCAT(recipient_id, ':', target_id, ':', available_at)
WHERE type = 'NOTIFICATION_DIGEST';

CREATE UNIQUE INDEX uk_notifications_digest_batch
    ON notifications(digest_batch_key);

CREATE UNIQUE INDEX uk_notification_email_digest_batch
    ON notification_email_outbox(recipient_user_id, available_at, digest_frequency);
