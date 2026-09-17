-- ============================================================
-- FLYWAY MIGRATION V100: CREATE NOTIFICATION CENTER SCHEMA
-- Epic: NCL-11 (Thông báo và nhắc việc)
-- Story: NCL-11-CN-001 (Trung tâm thông báo trong hệ thống)
-- ============================================================

-- 1. Bảng lưu trữ sự kiện thông báo (Idempotent theo source_event_key)
CREATE TABLE IF NOT EXISTS notification_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    level VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    related_entity_type VARCHAR(100) NULL,
    related_entity_id VARCHAR(100) NULL,
    source_event_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_notification_events_source_key
        UNIQUE (source_event_key),

    CONSTRAINT chk_notification_events_level
        CHECK (level IN ('CAO', 'TRUNG_BINH', 'THAP'))
);

-- 2. Bảng lưu trạng thái thông báo theo từng người nhận (Inbox Item)
CREATE TABLE IF NOT EXISTS notification_recipients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_event_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_recipients_event
        FOREIGN KEY (notification_event_id)
        REFERENCES notification_events(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notification_recipients_user
        FOREIGN KEY (recipient_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_notification_recipient_event_user
        UNIQUE (notification_event_id, recipient_user_id)
);

-- Chỉ mục tối ưu cho truy vấn hộp thư cá nhân và đếm unread count
CREATE INDEX idx_notif_recipients_query
    ON notification_recipients(recipient_user_id, is_deleted, is_read, created_at);

CREATE INDEX idx_notif_recipients_event
    ON notification_recipients(notification_event_id);

-- 3. Bảng lưu vết kiểm toán thao tác người dùng trên trung tâm thông báo (TC-03)
CREATE TABLE IF NOT EXISTS notification_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NULL,
    detail TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_audit_logs_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_notification_audit_action
        CHECK (action IN ('MARK_READ', 'MARK_ALL_READ', 'DELETE_NOTIFICATION'))
);

CREATE INDEX idx_notif_audit_logs_actor_created
    ON notification_audit_logs(actor_user_id, created_at);

-- 4. Di trú an toàn dữ liệu lịch sử từ bảng notifications cũ sang mô hình mới
-- Tất cả thông báo legacy được gán level = 'THAP' theo quyết định thiết kế
INSERT INTO notification_events (id, event_type, level, title, message, related_entity_type, related_entity_id, source_event_key, created_at)
SELECT
    n.id,
    COALESCE(n.type, 'LEGACY_NOTIFICATION'),
    'THAP',
    COALESCE(n.title, 'Thông báo'),
    COALESCE(n.content, n.title, ''),
    n.target_type,
    CAST(n.target_id AS CHAR),
    CONCAT('LEGACY:NOTIF:', n.id),
    n.created_at
FROM notifications n
WHERE NOT EXISTS (
    SELECT 1 FROM notification_events ne WHERE ne.source_event_key = CONCAT('LEGACY:NOTIF:', n.id)
);

INSERT INTO notification_recipients (id, notification_event_id, recipient_user_id, is_read, read_at, is_deleted, deleted_at, created_at)
SELECT
    n.id,
    n.id,
    n.recipient_id,
    n.is_read,
    CASE WHEN n.is_read = TRUE THEN n.created_at ELSE NULL END,
    FALSE,
    NULL,
    n.created_at
FROM notifications n
WHERE NOT EXISTS (
    SELECT 1 FROM notification_recipients nr WHERE nr.id = n.id
);
