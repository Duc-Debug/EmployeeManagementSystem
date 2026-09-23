-- ============================================================
-- FLYWAY MIGRATION V115: CREATE NOTIFICATION DEDUPLICATION SCHEMA
-- Epic: NCL-11 (Thông báo và nhắc việc)
-- Story: NCL-11-CN-003 (Chống gửi trùng thông báo)
-- ============================================================

-- 1. Thêm quyền NOTIFICATION_DEDUPLICATION_MANAGE cho VT-06 (Quản trị viên)
INSERT INTO permissions (code, name, description)
SELECT 'NOTIFICATION_DEDUPLICATION_MANAGE', 'Cấu hình chống gửi trùng thông báo', 'Cho phép Quản trị viên (VT-06) cấu hình quy tắc chống gửi trùng thông báo và tác vụ nền theo QTN-19'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'NOTIFICATION_DEDUPLICATION_MANAGE');

-- Gán quyền NOTIFICATION_DEDUPLICATION_MANAGE duy nhất cho VT-06
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'NOTIFICATION_DEDUPLICATION_MANAGE'
WHERE r.code = 'VT-06'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 2. Bảng lưu trữ cấu hình chống gửi trùng thông báo (TC-03, TC-04)
CREATE TABLE IF NOT EXISTS notification_dedup_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    dedup_window_days INT NOT NULL DEFAULT 7,
    scan_interval_minutes INT NOT NULL DEFAULT 60,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_notif_dedup_config_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_notif_dedup_window_days
        CHECK (dedup_window_days >= 1 AND dedup_window_days <= 90),

    CONSTRAINT chk_notif_dedup_scan_interval
        CHECK (scan_interval_minutes >= 5 AND scan_interval_minutes <= 1440)
);

-- Khởi tạo bản ghi cấu hình mặc định (singleton id = 1)
INSERT INTO notification_dedup_configs (id, is_enabled, dedup_window_days, scan_interval_minutes, version)
SELECT 1, TRUE, 7, 60, 0
WHERE NOT EXISTS (SELECT 1 FROM notification_dedup_configs WHERE id = 1);

-- 3. Bảng lưu vết lịch sử thay đổi cấu hình chống gửi trùng (TC-04)
CREATE TABLE IF NOT EXISTS notification_dedup_config_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL DEFAULT 'UPDATE_DEDUP_CONFIG',
    previous_value TEXT NULL,
    new_value TEXT NOT NULL,
    change_summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notif_dedup_histories_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_notif_dedup_histories_actor
    ON notification_dedup_config_histories(actor_user_id, created_at);

-- 4. Bảng lưu trữ khóa chống gửi trùng thông báo (Active/Status-based Dedup Record - TC-01, TC-02, QTN-19)
CREATE TABLE IF NOT EXISTS notification_dedup_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dedup_key VARCHAR(255) NOT NULL,
    active_dedup_key VARCHAR(255) NULL,
    event_type VARCHAR(100) NOT NULL,
    target_entity_type VARCHAR(100) NOT NULL,
    target_entity_id VARCHAR(100) NOT NULL,
    year_week VARCHAR(20) NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,

    CONSTRAINT fk_notif_dedup_recipient
        FOREIGN KEY (recipient_user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_notif_dedup_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')),

    -- Đảm bảo chỉ có tối đa 1 bản ghi ACTIVE cho cùng 1 dedup_key tại một thời điểm
    -- Khi chuyển sang INACTIVE, active_dedup_key được đặt về NULL
    CONSTRAINT uk_notif_dedup_active_key
        UNIQUE (active_dedup_key)
);

CREATE INDEX idx_notif_dedup_lookup
    ON notification_dedup_records(dedup_key, status);

CREATE INDEX idx_notif_dedup_entity_week
    ON notification_dedup_records(target_entity_type, target_entity_id, year_week, status);
