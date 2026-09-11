-- ============================================================
-- FLYWAY MIGRATION V56: CREATE RECRUITMENT DEMAND REPORT SCHEMA
-- Epic: NCL-10 (Báo cáo & Phân tích)
-- Story: NCL-10-CN-005 (Báo cáo nhu cầu tuyển dụng theo kỹ năng)
-- ============================================================

-- 1. Bổ sung Permission mới cho Báo cáo nhu cầu tuyển dụng
INSERT INTO permissions (code, name, description)
SELECT 'RECRUITMENT_DEMAND_REPORT_READ', 'Xem báo cáo nhu cầu tuyển dụng', 'Cho phép xem báo cáo tổng hợp nhu cầu tuyển dụng theo kỹ năng'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'RECRUITMENT_DEMAND_REPORT_READ'
);

-- 2. Gán quyền cho VT-01 (Ban Giám đốc), VT-03 (Quản lý nguồn lực / HR), VT-06 (Quản trị viên)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('VT-01', 'VT-03', 'VT-06')
  AND p.code = 'RECRUITMENT_DEMAND_REPORT_READ'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. Nạp danh mục kỹ năng Kiểm thử chuẩn (nếu chưa có)
INSERT INTO skills (code, name, category, description)
SELECT 'TESTING', 'Kiểm thử (Testing / QA)', 'Testing', 'Kiểm thử phần mềm, kiểm thử tự động và đảm bảo chất lượng'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE code IN ('TESTING', 'TEST'));
