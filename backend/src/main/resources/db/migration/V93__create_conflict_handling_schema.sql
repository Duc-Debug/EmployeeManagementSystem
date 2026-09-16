-- ============================================================
-- FLYWAY MIGRATION V93: CREATE CONFLICT HANDLING SCHEMA
-- Epic: NCL-07 (Quản lý & Theo dõi Phân bổ)
-- Story: NCL-07-CN-005 (Danh sách xung đột cần xử lý)
-- ============================================================

-- 1. Bổ sung Permission mới cho Quản lý & Xử lý danh sách xung đột lịch
INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_CONFLICT_HANDLE', 'Quản lý & Xử lý danh sách xung đột lịch', 'Cho phép phân công người xử lý, ghi cách xử lý và đánh dấu xử lý xung đột lịch'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'RESOURCE_CONFLICT_HANDLE'
);

-- 2. Gán quyền cho VT-03 (Quản lý nguồn lực - RM) và VT-06 (Admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('VT-03', 'VT-06')
  AND p.code = 'RESOURCE_CONFLICT_HANDLE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. Bổ sung các cột xử lý xung đột vào bảng schedule_conflict_warnings
ALTER TABLE schedule_conflict_warnings ADD COLUMN assigned_handler_id BIGINT NULL;
ALTER TABLE schedule_conflict_warnings ADD COLUMN resolution_note TEXT NULL;
ALTER TABLE schedule_conflict_warnings ADD COLUMN is_recurrent BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE schedule_conflict_warnings ADD COLUMN recurrent_note TEXT NULL;
ALTER TABLE schedule_conflict_warnings ADD COLUMN resolved_at TIMESTAMP NULL;
ALTER TABLE schedule_conflict_warnings ADD COLUMN resolved_by BIGINT NULL;
ALTER TABLE schedule_conflict_warnings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 4. Tạo ngoại khóa cho người chịu trách nhiệm xử lý
ALTER TABLE schedule_conflict_warnings
    ADD CONSTRAINT fk_conflict_assigned_handler
    FOREIGN KEY (assigned_handler_id) REFERENCES employees (id) ON DELETE SET NULL;

-- 5. Bảo tồn bản ghi trùng lặp (nếu có) vào bảng sao lưu trước khi làm sạch và tạo UNIQUE INDEX
CREATE TABLE IF NOT EXISTS schedule_conflict_warnings_duplicates_backup (
    id BIGINT NOT NULL,
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
    assigned_handler_id BIGINT NULL,
    resolution_note TEXT NULL,
    is_recurrent BOOLEAN NOT NULL DEFAULT FALSE,
    recurrent_note TEXT NULL,
    resolved_at TIMESTAMP NULL,
    resolved_by BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

INSERT INTO schedule_conflict_warnings_duplicates_backup (
    id, employee_id, year_number, week_number, conflict_type,
    project_ids, project_names, leave_request_id, leave_info,
    total_allocated_hours, net_available_hours, excess_hours,
    status, details, notified_at, notified_by, assigned_handler_id,
    resolution_note, is_recurrent, recurrent_note, resolved_at, resolved_by,
    version, created_at, updated_at
)
SELECT id, employee_id, year_number, week_number, conflict_type,
       project_ids, project_names, leave_request_id, leave_info,
       total_allocated_hours, net_available_hours, excess_hours,
       status, details, notified_at, notified_by, assigned_handler_id,
       resolution_note, is_recurrent, recurrent_note, resolved_at, resolved_by,
       version, created_at, updated_at
FROM schedule_conflict_warnings
WHERE id NOT IN (
    SELECT canonical_id FROM (
        SELECT id AS canonical_id,
               ROW_NUMBER() OVER (
                   PARTITION BY employee_id, year_number, week_number, conflict_type
                    ORDER BY 
                        updated_at DESC,
                        id DESC
               ) AS rn
        FROM schedule_conflict_warnings
    ) AS ranked
    WHERE rn = 1
);

DELETE FROM schedule_conflict_warnings
WHERE id IN (
    SELECT id FROM schedule_conflict_warnings_duplicates_backup
);

-- 6. Bổ sung index phục vụ truy vấn cho bảng schedule_conflict_warnings
CREATE INDEX idx_schedule_conflict_year_week ON schedule_conflict_warnings(year_number, week_number);
CREATE UNIQUE INDEX uk_schedule_conflict_existing ON schedule_conflict_warnings(employee_id, year_number, week_number, conflict_type);


