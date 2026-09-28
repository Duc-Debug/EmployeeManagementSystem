-- ============================================================
-- FLYWAY MIGRATION V53: ADD DEPARTMENT LEAVE CALENDAR PERMISSION (NCL-05-CN-006)
-- Tham chiếu: User Story NCL-05-CN-006 & RBAC Specification
-- ============================================================

-- 1. Insert DEPARTMENT_LEAVE_READ into permissions if not exists
INSERT INTO permissions (code, name, description)
SELECT 'DEPARTMENT_LEAVE_READ', 'Xem lịch nghỉ bộ phận theo tháng', 'Cho phép xem lịch nghỉ phép của toàn bộ nhân sự trong bộ phận theo tháng và cảnh báo số người nghỉ vượt ngưỡng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DEPARTMENT_LEAVE_READ');

-- 2. Grant DEPARTMENT_LEAVE_READ to VT-03 (Quản lý nguồn lực), VT-01 (Ban giám đốc), VT-06 (Quản trị viên)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'DEPARTMENT_LEAVE_READ'
  AND r.code IN ('VT-03', 'VT-01', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
