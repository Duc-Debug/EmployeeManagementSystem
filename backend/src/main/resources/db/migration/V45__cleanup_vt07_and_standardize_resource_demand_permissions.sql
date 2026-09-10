-- ============================================================
-- FLYWAY MIGRATION V44: CLEANUP VT-07 & STANDARDIZE RESOURCE DEMAND RBAC
-- Tham chiếu: docs/ROLE_BASED_ACCESS_CONTROL_GUIDE.md
--
-- 1. "Nhân viên công ty" KHÔNG PHẢI là một Role độc lập.
--    Hệ thống chỉ có 6 Role chính thức (VT-01 -> VT-06).
--    Xóa sạch bản ghi legacy VT-07 khỏi bảng roles.
--
-- 2. Ma trận phân quyền Quản lý dự án:
--    - VT-01 (Giám đốc): 👁️ Xem (Read-only) -> Thu hồi PROJECT_RESOURCE_DEMAND_ESTIMATE
--    - VT-02 (PM): ✅ Dự án của mình -> Đảm bảo có cả READ và ESTIMATE
--    - VT-03 (RM): 👁️ Xem -> Đảm bảo có READ
--    - VT-06 (Admin): ❌ Không có quyền -> Thu hồi cả READ và ESTIMATE
-- ============================================================

-- 1. Xóa liên kết của role VT-07 (nếu có)
DELETE FROM role_permissions 
WHERE role_id IN (SELECT id FROM roles WHERE code = 'VT-07');

DELETE FROM project_resource_demands 
WHERE role_id IN (SELECT id FROM roles WHERE code = 'VT-07');

DELETE FROM roles 
WHERE code = 'VT-07';

-- 2. Chuẩn hóa VT-01: Thuần túy xem (Read-only), thu hồi quyền tạo/sửa/xóa ước lượng
DELETE FROM role_permissions 
WHERE role_id IN (SELECT id FROM roles WHERE code = 'VT-01')
  AND permission_id IN (SELECT id FROM permissions WHERE code = 'PROJECT_RESOURCE_DEMAND_ESTIMATE');

-- 3. Chuẩn hóa VT-06: Không có quyền truy cập Quản lý dự án (❌), thu hồi cả READ và ESTIMATE
DELETE FROM role_permissions 
WHERE role_id IN (SELECT id FROM roles WHERE code = 'VT-06')
  AND permission_id IN (SELECT id FROM permissions WHERE code IN ('PROJECT_RESOURCE_DEMAND_READ', 'PROJECT_RESOURCE_DEMAND_ESTIMATE'));

-- 4. Đảm bảo VT-02 có đủ quyền PROJECT_RESOURCE_DEMAND_READ và PROJECT_RESOURCE_DEMAND_ESTIMATE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'VT-02'
  AND p.code IN ('PROJECT_RESOURCE_DEMAND_READ', 'PROJECT_RESOURCE_DEMAND_ESTIMATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 5. Đảm bảo VT-03 có quyền PROJECT_RESOURCE_DEMAND_READ
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
