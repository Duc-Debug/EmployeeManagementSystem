-- ============================================================
-- FLYWAY MIGRATION V103: ADD TIMESHEET VARIANCE REPORT PERMISSION
-- Epic: NCL-09 (Giờ làm thực tế và đối chiếu kế hoạch)
-- Story: NCL-09-CN-004 (Đối chiếu giờ phân bổ với giờ thực tế)
-- ============================================================

-- 1. Thêm Permission TIMESHEET_VARIANCE_READ
INSERT INTO permissions (code, name, description)
SELECT 'TIMESHEET_VARIANCE_READ', 'Xem đối chiếu giờ phân bổ vs thực tế', 'Cho phép Ban giám đốc (VT-01), Quản lý nguồn lực (VT-03) và Admin (VT-06) xem báo cáo đối chiếu giờ phân bổ và giờ công thực tế đã duyệt'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'TIMESHEET_VARIANCE_READ');

-- 2. Gán quyền TIMESHEET_VARIANCE_READ cho Ban Giám đốc (VT-01), Quản lý Nguồn lực (VT-03) và Admin (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'TIMESHEET_VARIANCE_READ'
  AND r.code IN ('VT-01', 'VT-03', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
