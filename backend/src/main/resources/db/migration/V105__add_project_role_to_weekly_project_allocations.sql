-- ============================================================
-- FLYWAY MIGRATION V105: ADD PROJECT ROLE ID TO WEEKLY PROJECT ALLOCATIONS
-- Epic: NCL-10 (Báo cáo phân bổ theo dự án & Quản lý phân bổ)
-- Story: NCL-10-CN-006 (Gán vai trò chuyên môn dự án cho từng dòng phân bổ)
-- ============================================================

-- 1. Bổ sung cột project_role_id vào weekly_project_allocations
ALTER TABLE weekly_project_allocations
ADD COLUMN project_role_id BIGINT NULL;

-- 2. Thêm khóa ngoại trỏ vào danh mục project_roles(id)
ALTER TABLE weekly_project_allocations
ADD CONSTRAINT fk_wpa_project_role
FOREIGN KEY (project_role_id) REFERENCES project_roles(id) ON DELETE RESTRICT;

-- 3. Tạo chỉ mục tối ưu hóa truy vấn theo dự án, vai trò và tuần
CREATE INDEX idx_wpa_project_role
ON weekly_project_allocations(project_id, project_role_id, year_number, week_number);
