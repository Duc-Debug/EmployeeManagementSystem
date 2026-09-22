-- ============================================================
-- FLYWAY MIGRATION V108: CREATE UNAVAILABILITY DECLARATIONS SCHEMA
-- Epic: NCL-13 (Cổng nhân viên và lịch cá nhân)
-- Story: NCL-13-CN-003 (Khai báo thời gian không sẵn sàng)
-- ============================================================

-- 1. Tạo bảng unavailability_declarations
CREATE TABLE IF NOT EXISTS unavailability_declarations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason_type VARCHAR(50) NOT NULL,
    reason_detail VARCHAR(500) NULL,
    total_hours_deducted DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approver_id BIGINT NULL,
    approver_comment VARCHAR(500) NULL,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_unavail_decl_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_unavail_decl_approver FOREIGN KEY (approver_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_unavail_dates CHECK (start_date <= end_date),
    CONSTRAINT chk_unavail_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

-- 2. Chỉ mục tối ưu hóa tìm kiếm
CREATE INDEX idx_unavail_emp_dates
    ON unavailability_declarations (employee_id, start_date, end_date);

CREATE INDEX idx_unavail_status
    ON unavailability_declarations (status);

-- 3. Bổ sung các permissions mới
INSERT INTO permissions (code, name, description)
SELECT 'UNAVAILABILITY_DECLARE', 'Khai báo thời gian không sẵn sàng', 'Cho phép nhân viên chuyên môn (VT-04) và các vai trò liên quan khai báo thời gian không sẵn sàng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'UNAVAILABILITY_DECLARE');

INSERT INTO permissions (code, name, description)
SELECT 'UNAVAILABILITY_APPROVE', 'Phê duyệt thời gian không sẵn sàng', 'Cho phép Quản lý nguồn lực (VT-03) và Admin (VT-06) phê duyệt hoặc từ chối thời gian không sẵn sàng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'UNAVAILABILITY_APPROVE');

INSERT INTO permissions (code, name, description)
SELECT 'UNAVAILABILITY_READ', 'Xem thời gian không sẵn sàng', 'Cho phép xem danh sách thời gian không sẵn sàng của nhân sự'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'UNAVAILABILITY_READ');

-- 4. Gán quyền UNAVAILABILITY_DECLARE cho VT-04, VT-03, VT-06
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'UNAVAILABILITY_DECLARE'
  AND r.code IN ('VT-03', 'VT-04', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 5. Gán quyền UNAVAILABILITY_APPROVE cho VT-03, VT-06
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'UNAVAILABILITY_APPROVE'
  AND r.code IN ('VT-03', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 6. Gán quyền UNAVAILABILITY_READ cho VT-01, VT-02, VT-03, VT-04, VT-05, VT-06
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'UNAVAILABILITY_READ'
  AND r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
