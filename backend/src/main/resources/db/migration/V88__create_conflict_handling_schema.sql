-- ============================================================
-- FLYWAY MIGRATION V88: CREATE CONFLICT HANDLING SCHEMA
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
ALTER TABLE schedule_conflict_warnings
    ADD COLUMN assigned_handler_id BIGINT NULL,
    ADD COLUMN resolution_note TEXT NULL,
    ADD COLUMN is_recurrent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN recurrent_note TEXT NULL,
    ADD COLUMN resolved_at TIMESTAMP NULL,
    ADD COLUMN resolved_by BIGINT NULL;

-- 4. Tạo ngoại khóa cho người chịu trách nhiệm xử lý
ALTER TABLE schedule_conflict_warnings
    ADD CONSTRAINT fk_conflict_assigned_handler
    FOREIGN KEY (assigned_handler_id) REFERENCES employees (id) ON DELETE SET NULL;
