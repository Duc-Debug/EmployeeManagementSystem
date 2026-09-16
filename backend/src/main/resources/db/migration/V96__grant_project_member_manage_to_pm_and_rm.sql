-- ============================================================
-- FLYWAY MIGRATION V92: GRANT PROJECT MEMBER MANAGE & RESOURCE SEARCH TO PM AND RM
-- Roles: VT-02 (Quản lý dự án / PM) and VT-03 (Quản lý nguồn lực / RM)
-- ============================================================

-- 1. Cấp quyền RESOURCE_SEARCH cho VT-02 (Quản lý dự án) để tìm kiếm ứng viên theo kỹ năng vào dự án
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'VT-02'
  AND p.code = 'RESOURCE_SEARCH'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 2. Cấp quyền PROJECT_UPDATE cho VT-03 (Quản lý nguồn lực) để thêm/xóa thành viên dự án phục vụ phân bổ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'VT-03'
  AND p.code = 'PROJECT_UPDATE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
