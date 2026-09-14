-- ============================================================
-- FLYWAY MIGRATION V75: CREATE TIMESHEETS & WORK LOGS SCHEMA
-- Epic: NCL-09 (Giờ làm thực tế và đối chiếu kế hoạch)
-- Story: NCL-09-CN-001 (Ghi giờ công theo công việc)
-- ============================================================

-- 1. Tạo bảng timesheets (Bảng chấm công theo tuần)
CREATE TABLE IF NOT EXISTS timesheets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    total_hours DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    submitted_at TIMESTAMP NULL,
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    rejection_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_timesheets_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_timesheets_approved_by
        FOREIGN KEY (approved_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_timesheets_employee_week
        UNIQUE (employee_id, week_start_date),

    CONSTRAINT chk_timesheets_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED')),

    CONSTRAINT chk_timesheets_total_hours
        CHECK (total_hours >= 0)
);

CREATE INDEX idx_timesheets_employee ON timesheets(employee_id);
CREATE INDEX idx_timesheets_week_range ON timesheets(week_start_date, week_end_date);
CREATE INDEX idx_timesheets_status ON timesheets(status);

-- 2. Tạo bảng timesheet_entries (Dòng ghi giờ công chi tiết)
CREATE TABLE IF NOT EXISTS timesheet_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timesheet_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    hours DECIMAL(5, 2) NOT NULL,
    is_billable BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_timesheet_entries_timesheet
        FOREIGN KEY (timesheet_id)
        REFERENCES timesheets(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_timesheet_entries_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_timesheet_entries_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_timesheet_entries_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_timesheet_entries_hours
        CHECK (hours > 0 AND hours <= 24),

    CONSTRAINT chk_timesheet_entries_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_timesheet_entries_timesheet ON timesheet_entries(timesheet_id);
CREATE INDEX idx_timesheet_entries_emp_date ON timesheet_entries(employee_id, work_date);
CREATE INDEX idx_timesheet_entries_project ON timesheet_entries(project_id);
CREATE INDEX idx_timesheet_entries_task ON timesheet_entries(task_id);

-- 3. Tạo các quyền ghi nhận giờ làm việc (Work Log / Timesheet Permissions)
INSERT INTO permissions (code, name, description)
SELECT 'WORK_LOG_CREATE', 'Ghi giờ công', 'Cho phép ghi số giờ làm việc thực tế cho công việc'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'WORK_LOG_CREATE');

INSERT INTO permissions (code, name, description)
SELECT 'WORK_LOG_READ', 'Xem giờ công cá nhân', 'Cho phép xem bảng chấm công và dòng ghi giờ cá nhân'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'WORK_LOG_READ');

INSERT INTO permissions (code, name, description)
SELECT 'WORK_LOG_UPDATE', 'Cập nhật giờ công', 'Cho phép sửa dòng giờ công ở trạng thái nháp'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'WORK_LOG_UPDATE');

INSERT INTO permissions (code, name, description)
SELECT 'WORK_LOG_DELETE', 'Xóa giờ công', 'Cho phép xóa dòng giờ công ở trạng thái nháp'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'WORK_LOG_DELETE');

-- 4. Gán quyền cho vai trò VT-04 (Nhân viên chuyên môn) và VT-06 (Quản trị viên)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('VT-04', 'VT-06')
  AND p.code IN ('WORK_LOG_CREATE', 'WORK_LOG_READ', 'WORK_LOG_UPDATE', 'WORK_LOG_DELETE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
