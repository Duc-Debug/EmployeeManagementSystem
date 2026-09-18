-- ============================================================
-- FLYWAY MIGRATION V109: CREATE EMPLOYEE SCHEDULE CONFIRMATION
-- Feature: NCL-13-CN-001 (Xem lịch phân bổ tuần của tôi)
-- ============================================================

CREATE TABLE IF NOT EXISTS employee_schedule_confirmation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    confirmed_at DATETIME(6) NOT NULL,
    ip_address VARCHAR(45) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_schedule_conf_user_week UNIQUE (user_id, week_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
