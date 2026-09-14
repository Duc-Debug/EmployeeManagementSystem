-- ============================================================
-- FLYWAY MIGRATION V73: CREATE SCHEDULE CONFLICT WARNINGS SCHEMA
-- Epic: NCL-07 (Quản lý & Theo dõi Phân bổ)
-- Story: NCL-07-CN-001 (Cảnh báo xung đột lịch của nhân sự)
-- ============================================================

-- 1. Bổ sung Permission mới cho Cảnh báo xung đột lịch
INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_SCHEDULE_CONFLICT_READ', 'Xem cảnh báo xung đột lịch', 'Cho phép xem và rà soát các cảnh báo xung đột lịch phân bổ và nghỉ phép'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'RESOURCE_SCHEDULE_CONFLICT_READ'
);

INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_SCHEDULE_CONFLICT_NOTIFY', 'Gửi thông báo & Xử lý xung đột lịch', 'Cho phép gửi thông báo thương lượng và xác nhận xử lý xung đột lịch'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'RESOURCE_SCHEDULE_CONFLICT_NOTIFY'
);

-- 2. Gán quyền cho VT-02 (PM), VT-03 (RM), VT-06 (Admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('VT-02', 'VT-03', 'VT-06')
  AND p.code IN ('RESOURCE_SCHEDULE_CONFLICT_READ', 'RESOURCE_SCHEDULE_CONFLICT_NOTIFY')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. Tạo bảng schedule_conflict_warnings
DROP TABLE IF EXISTS schedule_conflict_warnings;

CREATE TABLE schedule_conflict_warnings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year_number INT NOT NULL,
    week_number INT NOT NULL,
    conflict_type VARCHAR(50) NOT NULL,
    project_ids VARCHAR(255) NULL,
    project_names TEXT NULL,
    leave_request_id BIGINT NULL,
    leave_info VARCHAR(255) NULL,
    total_allocated_hours DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    net_available_hours DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    excess_hours DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    details TEXT NULL,
    notified_at TIMESTAMP NULL,
    notified_by BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_conflict_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_conflict_leave FOREIGN KEY (leave_request_id) REFERENCES leave_requests (id) ON DELETE SET NULL,
    INDEX idx_conflict_emp_week (employee_id, year_number, week_number),
    INDEX idx_conflict_status (status)
);
