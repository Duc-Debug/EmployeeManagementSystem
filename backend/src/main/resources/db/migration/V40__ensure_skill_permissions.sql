-- ============================================================
-- FLYWAY MIGRATION V36: ENSURE SKILL PERMISSIONS AND ASSIGNMENTS
-- ============================================================

-- 1. Đảm bảo các quyền quản trị danh mục kỹ năng tồn tại trong bảng permissions
INSERT INTO permissions (code, name, description)
SELECT 'SKILL_READ', 'Xem kỹ năng', 'Cho phép xem danh sách và chi tiết kỹ năng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SKILL_READ');

INSERT INTO permissions (code, name, description)
SELECT 'SKILL_CREATE', 'Tạo kỹ năng', 'Cho phép tạo kỹ năng và nhóm kỹ năng mới'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SKILL_CREATE');

INSERT INTO permissions (code, name, description)
SELECT 'SKILL_UPDATE', 'Cập nhật kỹ năng', 'Cho phép cập nhật thông tin kỹ năng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SKILL_UPDATE');

INSERT INTO permissions (code, name, description)
SELECT 'SKILL_MERGE', 'Gộp kỹ năng', 'Cho phép gộp các kỹ năng trùng lặp'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SKILL_MERGE');

INSERT INTO permissions (code, name, description)
SELECT 'SKILL_DEACTIVATE', 'Vô hiệu hóa kỹ năng', 'Cho phép vô hiệu hóa kỹ năng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SKILL_DEACTIVATE');

-- 2. Cấp toàn bộ quyền quản trị kỹ năng cho Quản trị viên (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VT-06' AND p.code IN ('SKILL_READ', 'SKILL_CREATE', 'SKILL_UPDATE', 'SKILL_MERGE', 'SKILL_DEACTIVATE')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 3. Cấp quyền xem kỹ năng SKILL_READ cho các vai trò khác (VT-01 -> VT-05)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05') AND p.code = 'SKILL_READ'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- 4. Cấp quyền xem kỹ năng nhân sự EMPLOYEE_SKILL_READ cho VT-01, VT-05, VT-06
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('VT-01', 'VT-05', 'VT-06') AND p.code = 'EMPLOYEE_SKILL_READ'
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
