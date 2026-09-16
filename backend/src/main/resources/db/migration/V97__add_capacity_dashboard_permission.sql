-- ============================================================
-- FLYWAY MIGRATION V97: ADD CAPACITY DASHBOARD PERMISSION
-- Epic: NCL-10 (Báo cáo năng lực và bảng điều khiển)
-- Story: NCL-10-CN-001 (Bảng điều khiển năng lực)
-- ============================================================

-- 1. Thêm Permission CAPACITY_DASHBOARD_READ
INSERT INTO permissions (code, name, description)
SELECT 'CAPACITY_DASHBOARD_READ', 'Xem bảng điều khiển năng lực', 'Cho phép Ban Giám Đốc (VT-01) và các Quản lý (VT-02, VT-03, VT-06) xem bảng điều khiển tổng quan về năng lực và phân bổ nguồn lực'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'CAPACITY_DASHBOARD_READ');

-- 2. Gán quyền CAPACITY_DASHBOARD_READ cho Ban Giám Đốc (VT-01), Quản lý Dự án (VT-02), Quản lý Nguồn lực (VT-03) và Admin (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'CAPACITY_DASHBOARD_READ'
  AND r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
