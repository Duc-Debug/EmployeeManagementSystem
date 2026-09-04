-- ============================================================
-- FLYWAY MIGRATION V19: ADD USER_UPDATE PERMISSION AND ASSIGN TO ADMIN
-- ============================================================

INSERT INTO permissions (code, name, description)
SELECT 'USER_UPDATE', 'Cập nhật tài khoản', 'Cho phép cập nhật thông tin tài khoản người dùng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'USER_UPDATE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'USER_UPDATE'
WHERE r.code = 'VT-06'
AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
);
