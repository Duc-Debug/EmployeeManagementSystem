-- ============================================================
-- FLYWAY MIGRATION V107: ADD DATA IMPORT PERMISSION
-- Epic: NCL-12 (Quản trị hệ thống và danh mục)
-- Story: NCL-12-CN-004 (Nhập dữ liệu nhân sự và dự án từ tệp)
-- ============================================================

-- 1. Thêm Permission DATA_IMPORT
INSERT INTO permissions (code, name, description)
SELECT 'DATA_IMPORT', 'Nhập dữ liệu từ tệp', 'Cho phép Quản trị viên (VT-06) nhập hàng loạt dữ liệu hồ sơ nhân sự và dự án từ tệp Excel/CSV'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DATA_IMPORT');

-- 2. Gán quyền DATA_IMPORT cho Quản trị viên (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'DATA_IMPORT'
  AND r.code = 'VT-06'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
