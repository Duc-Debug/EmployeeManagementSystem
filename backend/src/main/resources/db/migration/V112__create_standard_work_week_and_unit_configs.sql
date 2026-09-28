-- ============================================================
-- FLYWAY MIGRATION V112: CREATE STANDARD WORK WEEK AND UNIT CONFIGS
-- ============================================================

-- 1. Bảng cấu hình tuần làm việc chuẩn và đơn vị tính năng lực
CREATE TABLE IF NOT EXISTS standard_work_week_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scope_type VARCHAR(30) NOT NULL DEFAULT 'COMPANY',
    scope_key VARCHAR(100) NOT NULL,
    org_unit_id BIGINT NULL,
    capacity_unit VARCHAR(20) NOT NULL DEFAULT 'HOURS',
    week_start_day VARCHAR(20) NOT NULL DEFAULT 'MONDAY',
    standard_hours_per_day DECIMAL(4,2) NOT NULL DEFAULT 8.00,
    standard_hours_per_week DECIMAL(5,2) NOT NULL DEFAULT 40.00,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_sww_scope_type CHECK (scope_type IN ('COMPANY', 'ORG_UNIT')),
    CONSTRAINT chk_sww_capacity_unit CHECK (capacity_unit IN ('HOURS', 'DAYS', 'FTE')),
    CONSTRAINT chk_sww_start_day CHECK (week_start_day IN ('MONDAY', 'SUNDAY')),
    CONSTRAINT chk_sww_hours_day CHECK (standard_hours_per_day > 0 AND standard_hours_per_day <= 12),
    CONSTRAINT chk_sww_hours_week CHECK (standard_hours_per_week > 0 AND standard_hours_per_week <= 84),
    CONSTRAINT uk_sww_scope_key UNIQUE (scope_key),
    CONSTRAINT fk_sww_org_unit FOREIGN KEY (org_unit_id) REFERENCES org_units(id) ON DELETE CASCADE,
    CONSTRAINT fk_sww_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_sww_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 2. Bảng chi tiết 7 ngày trong tuần làm việc chuẩn
CREATE TABLE IF NOT EXISTS standard_work_week_days (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    is_working_day BOOLEAN NOT NULL DEFAULT TRUE,
    working_hours DECIMAL(4,2) NOT NULL DEFAULT 8.00,

    CONSTRAINT chk_sww_day_hours CHECK (
        (is_working_day = FALSE AND working_hours = 0) OR
        (is_working_day = TRUE AND working_hours > 0 AND working_hours <= 12)
    ),
    CONSTRAINT uk_sww_config_day UNIQUE (config_id, day_of_week),
    CONSTRAINT fk_sww_days_config FOREIGN KEY (config_id) REFERENCES standard_work_week_configs(id) ON DELETE CASCADE
);

-- Index tra cứu theo scope
CREATE INDEX idx_sww_scope_lookup ON standard_work_week_configs (scope_type, org_unit_id);

-- 3. Thêm Permissions
INSERT INTO permissions (code, name, description)
SELECT 'STANDARD_WORK_WEEK_READ', 'Xem cấu hình đơn vị và tuần làm việc chuẩn', 'Cho phép xem cấu hình tuần làm việc chuẩn, giờ chuẩn mỗi ngày và đơn vị đo lường năng lực'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'STANDARD_WORK_WEEK_READ');

INSERT INTO permissions (code, name, description)
SELECT 'STANDARD_WORK_WEEK_MANAGE', 'Quản lý cấu hình đơn vị và tuần làm việc chuẩn', 'Cho phép cấu hình đơn vị đo lường năng lực, ngày bắt đầu tuần, ngày làm việc và giờ làm việc chuẩn'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'STANDARD_WORK_WEEK_MANAGE');

-- 4. Gán quyền STANDARD_WORK_WEEK_READ cho tất cả các vai trò (VT-01 -> VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'STANDARD_WORK_WEEK_READ'
  AND r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 5. Gán quyền STANDARD_WORK_WEEK_MANAGE cho VT-05 (Nhân sự) và VT-06 (Admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'STANDARD_WORK_WEEK_MANAGE'
  AND r.code IN ('VT-05', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 6. Seed dữ liệu mặc định ban đầu cho cấp COMPANY
INSERT INTO standard_work_week_configs (
    id, scope_type, scope_key, org_unit_id, capacity_unit, week_start_day, standard_hours_per_day, standard_hours_per_week, created_by
)
SELECT 1, 'COMPANY', 'COMPANY:DEFAULT', NULL, 'HOURS', 'MONDAY', 8.00, 40.00, NULL
WHERE NOT EXISTS (SELECT 1 FROM standard_work_week_configs WHERE scope_key = 'COMPANY:DEFAULT');

INSERT INTO standard_work_week_days (config_id, day_of_week, is_working_day, working_hours)
SELECT 1, 'MONDAY', TRUE, 8.00
WHERE NOT EXISTS (SELECT 1 FROM standard_work_week_days WHERE config_id = 1 AND day_of_week = 'MONDAY');

INSERT INTO standard_work_week_days (config_id, day_of_week, is_working_day, working_hours)
SELECT 1, 'TUESDAY', TRUE, 8.00
WHERE NOT EXISTS (SELECT 1 FROM standard_work_week_days WHERE config_id = 1 AND day_of_week = 'TUESDAY');

INSERT INTO standard_work_week_days (config_id, day_of_week, is_working_day, working_hours)
SELECT 1, 'WEDNESDAY', TRUE, 8.00
WHERE NOT EXISTS (SELECT 1 FROM standard_work_week_days WHERE config_id = 1 AND day_of_week = 'WEDNESDAY');

INSERT INTO standard_work_week_days (config_id, day_of_week, is_working_day, working_hours)
SELECT 1, 'THURSDAY', TRUE, 8.00
WHERE NOT EXISTS (SELECT 1 FROM standard_work_week_days WHERE config_id = 1 AND day_of_week = 'THURSDAY');

INSERT INTO standard_work_week_days (config_id, day_of_week, is_working_day, working_hours)
SELECT 1, 'FRIDAY', TRUE, 8.00
WHERE NOT EXISTS (SELECT 1 FROM standard_work_week_days WHERE config_id = 1 AND day_of_week = 'FRIDAY');

INSERT INTO standard_work_week_days (config_id, day_of_week, is_working_day, working_hours)
SELECT 1, 'SATURDAY', FALSE, 0.00
WHERE NOT EXISTS (SELECT 1 FROM standard_work_week_days WHERE config_id = 1 AND day_of_week = 'SATURDAY');

INSERT INTO standard_work_week_days (config_id, day_of_week, is_working_day, working_hours)
SELECT 1, 'SUNDAY', FALSE, 0.00
WHERE NOT EXISTS (SELECT 1 FROM standard_work_week_days WHERE config_id = 1 AND day_of_week = 'SUNDAY');

