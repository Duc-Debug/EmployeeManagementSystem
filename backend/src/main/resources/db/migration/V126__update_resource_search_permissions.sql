-- ====================================================================
-- FLYWAY MIGRATION V126: RESTRICT RESOURCE_SEARCH TO VT-03 (RM) & VT-06 (ADMIN)
-- User Story: NCL-02-CN-004, NCL-07-CN-005, NCL-07-CN-006
-- ====================================================================

-- 1. Đảm bảo quyền RESOURCE_SEARCH tồn tại
INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_SEARCH', 'Tìm kiếm nhân sự theo kỹ năng và độ rảnh', 'Cho phép Quản lý nguồn lực tra cứu nhân sự theo khoảng giờ trống và kỹ năng chuyên môn'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RESOURCE_SEARCH');

-- 2. Xóa quyền RESOURCE_SEARCH khỏi các vai trò không có thẩm quyền: VT-02 (PM), VT-04 (Nhân viên), VT-05 (HR), VT-01 (BGĐ)
DELETE FROM role_permissions
WHERE permission_id IN (SELECT id FROM permissions WHERE code = 'RESOURCE_SEARCH')
  AND role_id IN (SELECT id FROM roles WHERE code IN ('VT-02', 'VT-04', 'VT-05', 'VT-01'));

-- 3. Gán quyền RESOURCE_SEARCH cho VT-03 (RM) và VT-06 (Admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'RESOURCE_SEARCH'
  AND r.code IN ('VT-03', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
