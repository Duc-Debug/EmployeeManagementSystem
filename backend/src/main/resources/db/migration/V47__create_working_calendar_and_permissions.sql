-- ============================================================
-- FLYWAY MIGRATION V47: CREATE WORKING CALENDAR AND PERMISSIONS (NCL-05-CN-001)
-- ============================================================

-- 1. Create working_calendar_configs table
CREATE TABLE IF NOT EXISTS working_calendar_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_code VARCHAR(50) NOT NULL DEFAULT 'DEFAULT',
    day_of_week VARCHAR(20) NOT NULL,
    is_working_day BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_working_calendar_day UNIQUE (company_code, day_of_week)
);

-- 2. Seed default 7 days of week (Mon-Fri working, Sat-Sun off)
INSERT INTO working_calendar_configs (company_code, day_of_week, is_working_day)
SELECT 'DEFAULT', 'MONDAY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM working_calendar_configs WHERE company_code = 'DEFAULT' AND day_of_week = 'MONDAY');

INSERT INTO working_calendar_configs (company_code, day_of_week, is_working_day)
SELECT 'DEFAULT', 'TUESDAY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM working_calendar_configs WHERE company_code = 'DEFAULT' AND day_of_week = 'TUESDAY');

INSERT INTO working_calendar_configs (company_code, day_of_week, is_working_day)
SELECT 'DEFAULT', 'WEDNESDAY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM working_calendar_configs WHERE company_code = 'DEFAULT' AND day_of_week = 'WEDNESDAY');

INSERT INTO working_calendar_configs (company_code, day_of_week, is_working_day)
SELECT 'DEFAULT', 'THURSDAY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM working_calendar_configs WHERE company_code = 'DEFAULT' AND day_of_week = 'THURSDAY');

INSERT INTO working_calendar_configs (company_code, day_of_week, is_working_day)
SELECT 'DEFAULT', 'FRIDAY', TRUE
WHERE NOT EXISTS (SELECT 1 FROM working_calendar_configs WHERE company_code = 'DEFAULT' AND day_of_week = 'FRIDAY');

INSERT INTO working_calendar_configs (company_code, day_of_week, is_working_day)
SELECT 'DEFAULT', 'SATURDAY', FALSE
WHERE NOT EXISTS (SELECT 1 FROM working_calendar_configs WHERE company_code = 'DEFAULT' AND day_of_week = 'SATURDAY');

INSERT INTO working_calendar_configs (company_code, day_of_week, is_working_day)
SELECT 'DEFAULT', 'SUNDAY', FALSE
WHERE NOT EXISTS (SELECT 1 FROM working_calendar_configs WHERE company_code = 'DEFAULT' AND day_of_week = 'SUNDAY');

-- 3. Insert permissions
INSERT INTO permissions (code, name, description)
SELECT 'WORKING_CALENDAR_READ', 'Xem lịch làm việc và ngày lễ', 'Cho phép xem cấu hình ngày làm việc trong tuần và danh sách ngày nghỉ lễ của công ty'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'WORKING_CALENDAR_READ');

INSERT INTO permissions (code, name, description)
SELECT 'WORKING_CALENDAR_MANAGE', 'Quản lý lịch làm việc và ngày lễ', 'Cho phép cấu hình ngày làm việc trong tuần, khai báo, chỉnh sửa và xóa ngày nghỉ lễ của công ty'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'WORKING_CALENDAR_MANAGE');

-- 4. Grant WORKING_CALENDAR_READ to all 6 official roles (VT-01 -> VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'WORKING_CALENDAR_READ'
  AND r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 5. Grant WORKING_CALENDAR_MANAGE to HR (VT-05) and System Admin (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'WORKING_CALENDAR_MANAGE'
  AND r.code IN ('VT-05', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
