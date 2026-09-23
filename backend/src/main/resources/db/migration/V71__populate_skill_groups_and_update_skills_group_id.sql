-- V71__populate_skill_groups_and_update_skills_group_id.sql
-- Phục hồi và chuẩn hóa phân loại nhóm kỹ năng (skill_groups) và khóa ngoại (skills.group_id)

-- =========================================================================
-- 1. Nạp danh mục nhóm kỹ năng chuẩn vào bảng skill_groups (tránh trùng lặp)
-- =========================================================================

INSERT INTO skill_groups (name, description, status)
SELECT 'Backend', 'Phát triển dịch vụ và xử lý nghiệp vụ phía máy chủ', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skill_groups WHERE name = 'Backend');

INSERT INTO skill_groups (name, description, status)
SELECT 'Frontend', 'Phát triển giao diện ứng dụng web và trải nghiệm người dùng', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skill_groups WHERE name = 'Frontend');

INSERT INTO skill_groups (name, description, status)
SELECT 'Database', 'Thiết kế, quản trị và tối ưu cơ sở dữ liệu', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skill_groups WHERE name = 'Database');

INSERT INTO skill_groups (name, description, status)
SELECT 'DevOps', 'Tự động hóa triển khai CI/CD, hạ tầng và Cloud', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skill_groups WHERE name = 'DevOps');

INSERT INTO skill_groups (name, description, status)
SELECT 'Mobile', 'Phát triển ứng dụng di động đa nền tảng (iOS / Android)', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skill_groups WHERE name = 'Mobile');

INSERT INTO skill_groups (name, description, status)
SELECT 'Testing & QA', 'Kiểm thử chất lượng phần mềm (Automation / Manual)', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skill_groups WHERE name = 'Testing & QA');

INSERT INTO skill_groups (name, description, status)
SELECT 'UI/UX Design', 'Thiết kế giao diện và trải nghiệm người dùng', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skill_groups WHERE name = 'UI/UX Design');


-- =========================================================================
-- 2. Cập nhật lại khóa ngoại group_id cho các skill hiện có dựa theo category cũ
-- (Dùng cú pháp ANSI Standard SQL tương thích cả MySQL và H2 Database trên GitHub CI)
-- =========================================================================

UPDATE skills
SET group_id = (
    SELECT g.id
    FROM skill_groups g
    WHERE g.name = skills.category
)
WHERE category IS NOT NULL
  AND EXISTS (
    SELECT 1
    FROM skill_groups g
    WHERE g.name = skills.category
  );
