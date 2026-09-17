-- ============================================================
-- FLYWAY MIGRATION V98: ADD SCENARIO COMPARE PERMISSION
-- Epic: NCL-08 (Mô phỏng kịch bản nhận thêm dự án)
-- Story: NCL-08-CN-004 (So sánh nhiều kịch bản)
-- ============================================================

-- 1. Thêm Permission RESOURCE_SCENARIO_COMPARE
INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_SCENARIO_COMPARE', 'So sánh nhiều kịch bản mô phỏng', 'Cho phép Ban giám đốc (VT-01) so sánh nhiều kịch bản mô phỏng nhận thêm dự án theo QTN-14'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RESOURCE_SCENARIO_COMPARE');

-- 2. Gán quyền RESOURCE_SCENARIO_COMPARE duy nhất cho Ban Giám đốc (VT-01)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'RESOURCE_SCENARIO_COMPARE'
WHERE r.code = 'VT-01'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
