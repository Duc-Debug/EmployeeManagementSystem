-- ============================================================
-- FLYWAY MIGRATION V52: BO SUNG CAC TRUONG PHE DUYET DON NGHI PHEP & PHAN QUYEN (NCL-05-CN-003)
-- ============================================================

-- 1. Bo sung cot approver_id va approver_comment vao bang leave_requests
ALTER TABLE leave_requests ADD COLUMN approver_id BIGINT NULL;
ALTER TABLE leave_requests ADD COLUMN approver_comment VARCHAR(500) NULL;

-- 2. Them ma quyen LEAVE_REQUEST_APPROVE vao bang permissions neu chua ton tai
INSERT INTO permissions (code, name, description)
SELECT 'LEAVE_REQUEST_APPROVE', 'Duyệt đơn nghỉ phép', 'Cho phép quản lý nguồn lực và nhân sự duyệt hoặc từ chối đơn xin nghỉ phép của nhân viên'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'LEAVE_REQUEST_APPROVE');

-- 3. Gan quyen LEAVE_REQUEST_APPROVE cho vai tro VT-03 (Quan ly nguon luc) theo TC-03
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'LEAVE_REQUEST_APPROVE'
WHERE r.code = 'VT-03'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 4. Gan quyen LEAVE_REQUEST_APPROVE cho vai tro VT-05 (Nhan su) de ho tro duyet cap cao
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'LEAVE_REQUEST_APPROVE'
WHERE r.code = 'VT-05'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
