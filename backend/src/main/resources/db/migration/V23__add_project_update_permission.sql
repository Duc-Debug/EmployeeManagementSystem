-- ============================================================
-- FLYWAY MIGRATION V23: ADD PROJECT_UPDATE PERMISSION
-- Epic: NCL-03 (Dự án và cây công việc)
-- Story: NCL-03-CN-001 (Cập nhật thông tin dự án)
-- ============================================================

INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_UPDATE', 'Cập nhật dự án', 'Cho phép chỉnh sửa thông tin dự án'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_UPDATE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'PROJECT_UPDATE'
WHERE r.code = 'VT-02'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);