-- ============================================================
-- FLYWAY MIGRATION V100: ADD PROJECT ALLOCATION REPORT PERMISSION
-- Epic: NCL-10 (Báo cáo năng lực và bảng điều khiển)
-- Story: NCL-10-CN-006 (Báo cáo phân bổ theo dự án)
-- ============================================================

-- 1. Thêm Permission PROJECT_ALLOCATION_REPORT_READ
INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_ALLOCATION_REPORT_READ', 'Xem báo cáo phân bổ theo dự án', 'Cho phép Quản lý dự án (VT-02), Ban Giám đốc (VT-01), Quản lý Nguồn lực (VT-03) và Admin (VT-06) xem báo cáo phân bổ nguồn lực theo dự án'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PROJECT_ALLOCATION_REPORT_READ');

-- 2. Gán quyền PROJECT_ALLOCATION_REPORT_READ cho Ban Giám Đốc (VT-01), Quản lý Dự án (VT-02), Quản lý Nguồn lực (VT-03) và Admin (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'PROJECT_ALLOCATION_REPORT_READ'
  AND r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );