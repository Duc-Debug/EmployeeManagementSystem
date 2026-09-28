-- ============================================================
-- FLYWAY MIGRATION V115: CREATE NOTIFICATION PREFERENCES SCHEMA
-- Epic: NCL-11 (Thông báo và nhắc việc)
-- Story: NCL-11-CN-002 (Cấu hình kênh và tần suất nhận thông báo)
-- ============================================================

CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    task_assigned_channel VARCHAR(20) NOT NULL DEFAULT 'ALL',
    task_due_reminder_channel VARCHAR(20) NOT NULL DEFAULT 'ALL',
    task_comment_channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP_ONLY',
    timesheet_reminder_channel VARCHAR(20) NOT NULL DEFAULT 'ALL',
    allocation_changed_channel VARCHAR(20) NOT NULL DEFAULT 'ALL',
    schedule_conflict_channel VARCHAR(20) NOT NULL DEFAULT 'ALL',
    frequency VARCHAR(30) NOT NULL DEFAULT 'IMMEDIATE',
    task_due_reminder_days INT NOT NULL DEFAULT 3,
    quiet_hours_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    quiet_hours_start TIME NULL,
    quiet_hours_end TIME NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_notif_pref_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT uq_notif_pref_user
        UNIQUE (user_id),

    CONSTRAINT chk_notif_pref_freq
        CHECK (frequency IN ('IMMEDIATE', 'DAILY_DIGEST', 'WEEKLY_DIGEST')),

    CONSTRAINT chk_notif_pref_task_due_days
        CHECK (task_due_reminder_days BETWEEN 1 AND 14)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notif_pref_user
    ON notification_preferences (user_id);
