-- ============================================================
-- FLYWAY MIGRATION V35: GRANT SKILL READ PERMISSIONS ACROSS ROLES
-- ============================================================

-- 1. Cấp quyền SKILL_READ cho các vai trò cần xem danh mục kỹ năng chuẩn:
-- VT-01 (Ban giám đốc), VT-02 (Quản lý dự án), VT-03 (Quản lý nguồn lực),
-- VT-04 (Nhân viên chuyên môn), VT-05 (Nhân sự)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05')
  AND p.code = 'SKILL_READ'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 2. Cấp quyền EMPLOYEE_SKILL_READ cho VT-01 (Ban giám đốc), VT-05 (Nhân sự), VT-06 (Quản trị viên)
-- để xem Ma trận kỹ năng bộ phận
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code IN ('VT-01', 'VT-05', 'VT-06')
  AND p.code = 'EMPLOYEE_SKILL_READ'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
