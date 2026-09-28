INSERT INTO permissions (code, name, description)
SELECT
    'PROJECT_READ',
    'Xem dự án',
    'Cho phép xem danh sách và thông tin dự án'
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE code = 'PROJECT_READ'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON p.code = 'PROJECT_READ'
WHERE r.code IN (
    'VT-01',
    'VT-02',
    'VT-03',
    'VT-04',
    'VT-06'
)
AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
);
