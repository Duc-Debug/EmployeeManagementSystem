-- ============================================================
-- FLYWAY MIGRATION V43: GRANT PROJECT_RESOURCE_DEMAND_READ TO VT-03
-- Tham chiếu: docs/ROLE_BASED_ACCESS_CONTROL_GUIDE.md — Ma trận Phân Quyền (dòng 45, 46)
--
-- VT-03 (Quản lý nguồn lực): Cần xem nhu cầu nhân sự của dự án để điều phối & giữ chỗ nhân sự theo tuần
-- ============================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'VT-03'
  AND p.code = 'PROJECT_RESOURCE_DEMAND_READ'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
