-- ============================================================
-- FLYWAY MIGRATION V89: ADD CAPACITY FORECAST REPORT PERMISSION
-- Epic: NCL-10 (Báo cáo năng lực và bảng điều khiển)
-- Story: NCL-10-CN-004 (Báo cáo dự báo năng lực các tuần tới)
-- ============================================================

-- 1. Thêm Permission CAPACITY_FORECAST_REPORT_READ
INSERT INTO permissions (code, name, description)
SELECT 'CAPACITY_FORECAST_REPORT_READ', 'Xem báo cáo dự báo năng lực', 'Cho phép Ban giám đốc (VT-01) và Quản lý nguồn lực (VT-03) xem báo cáo dự báo năng lực các tuần tương lai'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'CAPACITY_FORECAST_REPORT_READ');

-- 2. Gán quyền CAPACITY_FORECAST_REPORT_READ cho Ban Giám đốc (VT-01), Quản lý Nguồn lực (VT-03) và Admin (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'CAPACITY_FORECAST_REPORT_READ'
  AND r.code IN ('VT-01', 'VT-03', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
