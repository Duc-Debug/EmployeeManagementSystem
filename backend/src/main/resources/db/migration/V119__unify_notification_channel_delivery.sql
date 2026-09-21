ALTER TABLE notification_digest_items
    ADD COLUMN channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP';

ALTER TABLE notification_email_outbox
    ADD COLUMN source_event_key VARCHAR(255) NULL;

CREATE UNIQUE INDEX uk_notification_email_immediate_source
    ON notification_email_outbox(recipient_user_id, source_event_key);

CREATE TABLE notification_email_digest_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    outbox_id BIGINT NOT NULL,
    source_event_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_email_digest_item_source
        UNIQUE (outbox_id, source_event_key),
    CONSTRAINT fk_notification_email_digest_item_outbox
        FOREIGN KEY (outbox_id) REFERENCES notification_email_outbox(id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_email_digest_items_outbox
    ON notification_email_digest_items(outbox_id, created_at);
