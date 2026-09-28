-- ============================================================
-- FLYWAY MIGRATION V106: ADD BILLABLE HOURS REPORT PERMISSION
-- Epic: NCL-10 (Báo cáo năng lực và bảng điều khiển)
-- Story: NCL-10-CN-002 (Báo cáo tỷ lệ giờ tính phí)
-- ============================================================

-- 1. Thêm Permission BILLABLE_HOURS_REPORT_READ
INSERT INTO permissions (code, name, description)
SELECT 'BILLABLE_HOURS_REPORT_READ', 'Xem báo cáo tỷ lệ giờ tính phí', 'Cho phép Ban giám đốc (VT-01), Quản lý nguồn lực (VT-03) và Admin (VT-06) xem báo cáo tỷ lệ giờ tính phí theo toàn công ty, bộ phận và nhân sự'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'BILLABLE_HOURS_REPORT_READ');

-- 2. Gán quyền BILLABLE_HOURS_REPORT_READ cho Ban Giám Đốc (VT-01), Quản lý Nguồn lực (VT-03) và Admin (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'BILLABLE_HOURS_REPORT_READ'
  AND r.code IN ('VT-01', 'VT-03', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
