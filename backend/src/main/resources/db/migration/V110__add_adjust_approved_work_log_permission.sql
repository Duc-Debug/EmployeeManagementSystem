-- ============================================================
-- FLYWAY MIGRATION V110: Add Adjust Approved Work Log Permission
-- Epic: NCL-09 (Giờ làm thực tế và đối chiếu kế hoạch)
-- Story: NCL-09-CN-005 (Điều chỉnh giờ làm đã duyệt)
-- ============================================================

-- 1. Tạo quyền điều chỉnh giờ làm đã duyệt
INSERT INTO permissions (code, name, description)
SELECT 'WORK_LOG_ADJUST', 'Điều chỉnh giờ làm đã duyệt', 'Cho phép quản lý dự án hoặc quản trị viên điều chỉnh dòng giờ công đã được duyệt kèm lý do giải trình'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'WORK_LOG_ADJUST');

-- 2. Gán quyền cho vai trò VT-02 (Quản trị dự án) và VT-06 (Quản trị viên)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('VT-02', 'VT-06')
  AND p.code = 'WORK_LOG_ADJUST'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
