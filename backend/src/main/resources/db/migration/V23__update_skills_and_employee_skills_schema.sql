-- ============================================================
-- FLYWAY MIGRATION V20: UPDATE SKILLS & EMPLOYEE SKILLS SCHEMA
-- ============================================================

-- 1. Xóa các bảng kỹ năng cũ (bảng trung gian v18 chưa có dữ liệu nghiệp vụ)
DROP TABLE IF EXISTS employee_skills;
DROP TABLE IF EXISTS skills;

-- 2. Tái tạo bảng skills với đầy đủ các trường mã chuẩn (code), phân loại (category) và nhóm (group_id)
CREATE TABLE skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NULL,
    description TEXT NULL,
    group_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    merged_into_skill_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_skills_group FOREIGN KEY (group_id) REFERENCES skill_groups(id),
    CONSTRAINT fk_skills_merged_into FOREIGN KEY (merged_into_skill_id) REFERENCES skills(id)
);

-- 3. Tái tạo bảng employee_skills thành Aggregate Root có ID tự tăng và đầy đủ trường nghiệp vụ
CREATE TABLE employee_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency_level INT NOT NULL,
    years_of_experience DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    rejection_reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_emp_skill_proficiency CHECK (proficiency_level BETWEEN 1 AND 5),
    CONSTRAINT fk_emp_skill_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_emp_skill_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    CONSTRAINT uq_employee_skill UNIQUE (employee_id, skill_id)
);

-- 4. Nạp danh mục kỹ năng chuẩn mẫu
INSERT INTO skills (code, name, category, description)
SELECT 'JAVA', 'Java', 'Backend', 'Ngôn ngữ lập trình Java'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE code = 'JAVA');

INSERT INTO skills (code, name, category, description)
SELECT 'SPRING_BOOT', 'Spring Boot', 'Backend', 'Framework Java Spring Boot'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE code = 'SPRING_BOOT');

INSERT INTO skills (code, name, category, description)
SELECT 'REACT', 'React.js', 'Frontend', 'Thư viện UI React.js'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE code = 'REACT');

INSERT INTO skills (code, name, category, description)
SELECT 'TYPESCRIPT', 'TypeScript', 'Frontend', 'Ngôn ngữ TypeScript'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE code = 'TYPESCRIPT');

INSERT INTO skills (code, name, category, description)
SELECT 'MYSQL', 'MySQL Database', 'Database', 'Hệ quản trị CSDL MySQL'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE code = 'MYSQL');

INSERT INTO skills (code, name, category, description)
SELECT 'DOCKER', 'Docker', 'DevOps', 'Công nghệ Containerization'
WHERE NOT EXISTS (SELECT 1 FROM skills WHERE code = 'DOCKER');

-- 5. Bổ sung các Permissions mới cho hồ sơ kỹ năng
INSERT INTO permissions (code, name, description)
SELECT 'EMPLOYEE_SKILL_READ', 'Xem kỹ năng nhân sự', 'Quyền xem hồ sơ kỹ năng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'EMPLOYEE_SKILL_READ');

INSERT INTO permissions (code, name, description)
SELECT 'EMPLOYEE_SKILL_DECLARE', 'Khai báo kỹ năng cá nhân', 'Quyền khai báo kỹ năng dành cho nhân viên'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'EMPLOYEE_SKILL_DECLARE');

INSERT INTO permissions (code, name, description)
SELECT 'EMPLOYEE_SKILL_APPROVE', 'Phê duyệt kỹ năng nhân sự', 'Quyền duyệt kỹ năng dành cho RM'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'EMPLOYEE_SKILL_APPROVE');

-- Cấp quyền cho VT-04 (Nhân viên chuyên môn)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VT-04' AND p.code IN ('EMPLOYEE_SKILL_READ', 'EMPLOYEE_SKILL_DECLARE')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Cấp quyền cho VT-03 (Quản lý nguồn lực)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VT-03' AND p.code IN ('EMPLOYEE_SKILL_READ', 'EMPLOYEE_SKILL_APPROVE')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);
