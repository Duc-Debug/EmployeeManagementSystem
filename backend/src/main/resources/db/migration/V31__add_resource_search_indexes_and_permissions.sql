-- ============================================================
-- FLYWAY MIGRATION V30: RESOURCE SEARCH BY SKILL & AVAILABILITY (NCL-02-CN-004)
-- ============================================================

-- 1. Thêm chỉ mục tăng tốc tìm kiếm nhân sự theo kỹ năng đã được duyệt và level
CREATE INDEX idx_emp_skills_search 
    ON employee_skills (skill_id, status, proficiency_level);

-- 2. Thêm chỉ mục trên bảng phân bổ dự án theo tuần để tính tổng giờ phân bổ nhanh
CREATE INDEX idx_wpa_emp_week 
    ON weekly_project_allocations (employee_id, year_number, week_number);

-- 3. Bổ sung quyền RESOURCE_SEARCH (nếu chưa có)
INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_SEARCH', 'Tìm kiếm nhân sự theo kỹ năng và độ rảnh', 'Cho phép Quản lý nguồn lực tìm nhân sự theo kỹ năng và giờ trống'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RESOURCE_SEARCH');

-- 4. Gán quyền RESOURCE_SEARCH cho VT-03 (Quản lý nguồn lực) và VT-01 (Ban giám đốc)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id 
FROM roles r, permissions p
WHERE r.code IN ('VT-03', 'VT-01') 
  AND p.code = 'RESOURCE_SEARCH'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );