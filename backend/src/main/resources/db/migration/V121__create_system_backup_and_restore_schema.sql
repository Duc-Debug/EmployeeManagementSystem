-- ============================================================
-- FLYWAY MIGRATION V121: CREATE SYSTEM BACKUP AND RESTORE SCHEMA
-- Epic: NCL-12 (Quản trị hệ thống và cấu hình nâng cao)
-- Story: NCL-12-CN-003 (Sao lưu và phục hồi dữ liệu)
-- ============================================================

-- 1. Thêm Permission DATA_BACKUP_MANAGE
INSERT INTO permissions (code, name, description)
SELECT 'DATA_BACKUP_MANAGE', 'Quản lý sao lưu và phục hồi dữ liệu', 'Cho phép Quản trị viên (VT-06) tạo sao lưu, phục hồi dữ liệu, cấu hình lịch tự động và quản lý các bản sao lưu hệ thống'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DATA_BACKUP_MANAGE');

-- 2. Gán quyền DATA_BACKUP_MANAGE cho Quản trị viên (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'DATA_BACKUP_MANAGE'
  AND r.code = 'VT-06'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. Tạo bảng quản lý các bản sao lưu system_backups
CREATE TABLE IF NOT EXISTS system_backups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    backup_code VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    backup_type VARCHAR(32) NOT NULL DEFAULT 'FULL',
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    checksum VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    is_automatic BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT NULL,
    created_by_name VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    error_message TEXT NULL,
    INDEX idx_backup_status (status),
    INDEX idx_backup_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tạo bảng cấu hình lịch sao lưu tự động backup_schedules
CREATE TABLE IF NOT EXISTS backup_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    frequency VARCHAR(32) NOT NULL DEFAULT 'DAILY',
    scheduled_time VARCHAR(8) NOT NULL DEFAULT '02:00',
    day_of_week VARCHAR(16) NULL DEFAULT 'MONDAY',
    backup_type VARCHAR(32) NOT NULL DEFAULT 'FULL',
    retention_days INT NOT NULL DEFAULT 30,
    last_run_at DATETIME NULL,
    next_run_at DATETIME NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Khởi tạo cấu hình lịch mặc định nếu chưa có
INSERT INTO backup_schedules (id, is_enabled, frequency, scheduled_time, day_of_week, backup_type, retention_days, updated_at)
SELECT 1, FALSE, 'DAILY', '02:00', 'MONDAY', 'FULL', 30, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM backup_schedules WHERE id = 1);

-- 5. Bảng nhật ký kiểm toán sao lưu & phục hồi backup_audit_logs
CREATE TABLE IF NOT EXISTS backup_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    user_email VARCHAR(255) NULL,
    action VARCHAR(64) NOT NULL,
    backup_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    reason TEXT NULL,
    details TEXT NULL,
    ip_address VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_backup_audit_created_at (created_at),
    INDEX idx_backup_audit_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
