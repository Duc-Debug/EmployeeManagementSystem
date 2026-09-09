INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_ALLOCATION_READ', 'Xem phân bổ nguồn lực', 'Xem công suất và phân bổ nguồn lực theo tuần'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RESOURCE_ALLOCATION_READ');

INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_ALLOCATION_MANAGE', 'Quản lý phân bổ nguồn lực', 'Điều chỉnh phân bổ nguồn lực theo tuần'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RESOURCE_ALLOCATION_MANAGE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'RESOURCE_ALLOCATION_READ'
WHERE r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'RESOURCE_ALLOCATION_MANAGE'
WHERE r.code = 'VT-03'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
