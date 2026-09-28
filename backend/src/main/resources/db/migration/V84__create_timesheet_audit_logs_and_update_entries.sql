-- ============================================================
-- FLYWAY MIGRATION V84: Create Timesheet Audit Logs & Update Entries
-- Epic: NCL-09 (Giờ làm thực tế và đối chiếu kế hoạch)
-- Story: NCL-09-CN-003 (Duyệt bảng chấm công)
-- ============================================================

-- 1. Thêm cột rejection_reason vào bảng timesheet_entries
ALTER TABLE timesheet_entries
ADD COLUMN rejection_reason TEXT NULL;

-- 2. Tạo bảng timesheet_audit_logs để lưu vết lịch sử chấm công
CREATE TABLE IF NOT EXISTS timesheet_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timesheet_id BIGINT NOT NULL,
    entry_id BIGINT NULL,
    action VARCHAR(50) NOT NULL,
    actor_id BIGINT NOT NULL,
    note TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ts_audit_timesheet
        FOREIGN KEY (timesheet_id)
        REFERENCES timesheets(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ts_audit_entry
        FOREIGN KEY (entry_id)
        REFERENCES timesheet_entries(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_ts_audit_actor
        FOREIGN KEY (actor_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    INDEX idx_ts_audit_timesheet (timesheet_id),
    INDEX idx_ts_audit_entry (entry_id)
);

-- 3. Tạo quyền duyệt giờ công
INSERT INTO permissions (code, name, description)
SELECT 'WORK_LOG_APPROVE', 'Duyệt giờ công', 'Cho phép quản lý dự án duyệt hoặc từ chối các dòng giờ công thuộc dự án phụ trách'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'WORK_LOG_APPROVE');

-- 4. Gán quyền cho vai trò VT-02 (Quản trị dự án)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'VT-02'
  AND p.code = 'WORK_LOG_APPROVE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
