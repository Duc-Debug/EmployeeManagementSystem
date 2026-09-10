-- ============================================================
-- FLYWAY MIGRATION V47: BO SUNG THONG TIN DON NGHI PHEP (NCL-05-CN-002)
-- ============================================================

-- 1. Bo sung cac cot cho bang leave_requests
ALTER TABLE leave_requests
    ADD COLUMN leave_type VARCHAR(50) NOT NULL DEFAULT 'ANNUAL' AFTER employee_id,
    ADD COLUMN reason VARCHAR(500) NULL AFTER hours_deducted,
    ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

-- 2. Dat rang buoc kiem tra hop le cho leave_type
ALTER TABLE leave_requests
    ADD CONSTRAINT chk_leave_requests_leave_type
    CHECK (leave_type IN ('ANNUAL', 'UNPAID', 'SICK', 'PERSONAL'));

-- 3. Them ma quyen LEAVE_REQUEST_CREATE vao bang permissions neu chua ton tai
INSERT INTO permissions (code, name, description)
SELECT 'LEAVE_REQUEST_CREATE', 'Gửi đơn nghỉ phép', 'Cho phép nhân viên chuyên môn nộp đơn xin nghỉ phép cá nhân'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'LEAVE_REQUEST_CREATE');

-- 4. Gan quyen LEAVE_REQUEST_CREATE cho vai tro VT-04 (Nhan vien chuyen mon) theo TC-04
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'LEAVE_REQUEST_CREATE'
WHERE r.code = 'VT-04'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
