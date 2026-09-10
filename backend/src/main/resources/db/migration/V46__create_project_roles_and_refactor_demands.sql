-- ============================================================
-- FLYWAY MIGRATION V46: CREATE PROJECT ROLES AND REFACTOR DEMANDS
-- Story: NCL-03-CN-007 (Ước lượng nhu cầu nhân sự theo vai trò)
--
-- Tách biệt hoàn toàn:
-- 1. roles: RBAC Security Roles (VT-01..VT-06) cho phân quyền hệ thống.
-- 2. project_roles: Vai trò chuyên môn dự án (DEV, TEST, BA, UIUX, PM, DEVOPS) cho hoạch định & phân bổ nhân sự.
-- ============================================================

-- 1. Tạo bảng danh mục Vai trò Chuyên môn Dự án
CREATE TABLE IF NOT EXISTS project_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Nạp dữ liệu các vai trò chuyên môn chuẩn cho sản xuất phần mềm/dự án
INSERT INTO project_roles (code, name, description)
SELECT 'DEV', 'Lập trình (Developer)', 'Phát triển mã nguồn, xây dựng tính năng và xử lý lỗi kỹ thuật'
WHERE NOT EXISTS (SELECT 1 FROM project_roles WHERE code = 'DEV');

INSERT INTO project_roles (code, name, description)
SELECT 'TEST', 'Kiểm thử (Tester / QA)', 'Kiểm thử chức năng, hiệu năng, bảo đảm chất lượng phần mềm'
WHERE NOT EXISTS (SELECT 1 FROM project_roles WHERE code = 'TEST');

INSERT INTO project_roles (code, name, description)
SELECT 'BA', 'Phân tích nghiệp vụ (Business Analyst)', 'Khảo sát yêu cầu, phân tích quy trình và viết tài liệu nghiệp vụ'
WHERE NOT EXISTS (SELECT 1 FROM project_roles WHERE code = 'BA');

INSERT INTO project_roles (code, name, description)
SELECT 'UIUX', 'Thiết kế (UI/UX Designer)', 'Thiết kế trải nghiệm người dùng, giao diện và luồng tương tác'
WHERE NOT EXISTS (SELECT 1 FROM project_roles WHERE code = 'UIUX');

INSERT INTO project_roles (code, name, description)
SELECT 'PM', 'Quản lý dự án (Project Manager)', 'Lập kế hoạch, điều phối nguồn lực và quản trị tiến độ dự án'
WHERE NOT EXISTS (SELECT 1 FROM project_roles WHERE code = 'PM');

INSERT INTO project_roles (code, name, description)
SELECT 'DEVOPS', 'Kỹ sư hệ thống (DevOps Engineer)', 'Triển khai hạ tầng CI/CD, máy chủ, bảo mật và vận hành hệ thống'
WHERE NOT EXISTS (SELECT 1 FROM project_roles WHERE code = 'DEVOPS');

-- 3. Xóa khóa ngoại cũ trỏ vào roles(id) (đã được tạo tường minh tại V30)
ALTER TABLE project_resource_demands DROP CONSTRAINT fk_proj_res_demands_role;

-- 4. Chuyển đổi an toàn role_id cũ từ roles sang project_roles tương ứng
UPDATE project_resource_demands prd
SET role_id = (
    SELECT pr.id FROM project_roles pr
    WHERE pr.code = CASE
        WHEN (SELECT r.code FROM roles r WHERE r.id = prd.role_id) = 'VT-02' THEN 'PM'
        ELSE 'DEV'
    END
)
WHERE EXISTS (
    SELECT 1 FROM roles r WHERE r.id = prd.role_id
);

-- 5. Khử trùng lặp (nếu có) sau khi map role_id để bảo đảm tính duy nhất (project_id, role_id, year_number, week_number)
DELETE FROM project_resource_demands
WHERE id NOT IN (
    SELECT min_id FROM (
        SELECT MIN(id) AS min_id
        FROM project_resource_demands
        GROUP BY project_id, role_id, year_number, week_number
    ) t
);

-- 6. Tạo khóa ngoại mới trỏ vào project_roles(id)
ALTER TABLE project_resource_demands
    ADD CONSTRAINT fk_proj_res_demands_project_role
    FOREIGN KEY (role_id) REFERENCES project_roles(id) ON DELETE RESTRICT;
