-- ============================================================
-- FLYWAY MIGRATION V35: ORG_UNIT_MANAGE & ROLE PERMISSION ADJUSTMENTS
-- ============================================================

-- 1. Them permission ORG_UNIT_MANAGE neu chua co
INSERT INTO permissions (code, name, description)
SELECT 'ORG_UNIT_MANAGE', 'Quản lý cơ cấu tổ chức', 'Cho phép thêm, sửa, xóa, chuyển vị trí và kích hoạt/vô hiệu hóa đơn vị tổ chức'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'ORG_UNIT_MANAGE');

-- 2. Gan ORG_UNIT_MANAGE cho VT-06 (System Admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'ORG_UNIT_MANAGE'
WHERE r.code = 'VT-06'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. Gan ORG_UNIT_READ cho tat ca cac vai tro (VT-01, VT-02, VT-03, VT-04, VT-05, VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'ORG_UNIT_READ'
  AND r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 4. Gan EMPLOYEE_READ cho tat ca cac vai tro (VT-01, VT-02, VT-03, VT-04, VT-05, VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'EMPLOYEE_READ'
  AND r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 5. Dam bao EMPLOYEE_UPDATE duoc gan cho VT-05 (HR Admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'EMPLOYEE_UPDATE'
WHERE r.code = 'VT-05'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 6. Thu hoi quyen EMPLOYEE_UPDATE khoi VT-06 (System Admin chi quan ly tai khoan, khong sua ho so nhan su)
DELETE FROM role_permissions
WHERE role_id IN (SELECT id FROM roles WHERE code = 'VT-06')
  AND permission_id IN (SELECT id FROM permissions WHERE code = 'EMPLOYEE_UPDATE');