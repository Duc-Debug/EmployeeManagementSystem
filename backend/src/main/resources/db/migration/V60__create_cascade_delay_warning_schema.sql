-- ============================================================
-- FLYWAY MIGRATION V60: CREATE CASCADE DELAY WARNING SCHEMA
-- Epic: NCL-04 (Quản lý Tiến độ & Công việc Dự án)
-- Story: NCL-04-CN-005 (Cảnh báo trễ dây chuyền khi một công việc trượt)
-- ============================================================

-- 1. Bổ sung cột actual_end_date và slack_days cho bảng tasks (TC-01, TC-02)
-- Lưu ý: Các cột này có thể đã tồn tại nếu database đã được cập nhật trước.
--         Script được viết để tương thích idempotent với cả hai trường hợp.
SET @col_exists_actual_end = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tasks'
      AND COLUMN_NAME = 'actual_end_date'
);

SET @col_exists_slack_days = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tasks'
      AND COLUMN_NAME = 'slack_days'
);

SET @sql_actual_end = IF(@col_exists_actual_end = 0,
    'ALTER TABLE tasks ADD COLUMN actual_end_date DATE NULL',
    'SELECT 1 -- actual_end_date already exists'
);

SET @sql_slack_days = IF(@col_exists_slack_days = 0,
    'ALTER TABLE tasks ADD COLUMN slack_days INT NOT NULL DEFAULT 0',
    'SELECT 1 -- slack_days already exists'
);

PREPARE stmt_actual_end FROM @sql_actual_end;
EXECUTE stmt_actual_end;
DEALLOCATE PREPARE stmt_actual_end;

PREPARE stmt_slack_days FROM @sql_slack_days;
EXECUTE stmt_slack_days;
DEALLOCATE PREPARE stmt_slack_days;

-- 2. Thêm các quyền cảnh báo trễ dây chuyền (TC-03)
INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_CASCADE_DELAY_READ', 'Xem cảnh báo trễ dây chuyền', 'Cho phép xem ảnh hưởng trễ dây chuyền khi công việc bị trượt'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_CASCADE_DELAY_READ'
);

INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_CASCADE_DELAY_MANAGE', 'Quản lý cảnh báo trễ dây chuyền', 'Cho phép cập nhật ngày kết thúc thực tế và xác nhận điều chỉnh trễ dây chuyền'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_CASCADE_DELAY_MANAGE'
);

-- 3. Gán quyền cho vai trò PM (VT-02) và Quản trị viên (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('PROJECT_CASCADE_DELAY_READ', 'PROJECT_CASCADE_DELAY_MANAGE')
WHERE r.code IN ('VT-02', 'VT-06')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);