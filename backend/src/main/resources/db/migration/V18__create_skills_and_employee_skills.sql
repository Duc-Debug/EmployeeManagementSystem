-- 1. Bảng danh mục kỹ năng chuẩn
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NULL,
    description TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng kỹ năng nhân viên (TC-01, TC-02)
CREATE TABLE IF NOT EXISTS employee_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency_level INT NOT NULL,
    years_of_experience DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    rejection_reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    
    CONSTRAINT chk_emp_skill_proficiency CHECK (proficiency_level BETWEEN 1 AND 5),
    CONSTRAINT fk_emp_skill_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_emp_skill_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    -- Ràng buộc chống dữ liệu trùng lặp (TC-02)
    CONSTRAINT uq_employee_skill UNIQUE (employee_id, skill_id)
);

-- 3. Nạp dữ liệu danh mục kỹ năng mẫu
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

-- 4. Thêm các quyền mới & cấp quyền
INSERT INTO permissions (code, name, description)
SELECT 'EMPLOYEE_SKILL_READ', 'Xem kỹ năng nhân sự', 'Quyền xem hồ sơ kỹ năng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'EMPLOYEE_SKILL_READ');

INSERT INTO permissions (code, name, description)
SELECT 'EMPLOYEE_SKILL_DECLARE', 'Khai báo kỹ năng cá nhân', 'Quyền khai báo kỹ năng dành cho nhân viên'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'EMPLOYEE_SKILL_DECLARE');

INSERT INTO permissions (code, name, description)
SELECT 'EMPLOYEE_SKILL_APPROVE', 'Phê duyệt kỹ năng nhân sự', 'Quyền duyệt kỹ năng dành cho RM'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'EMPLOYEE_SKILL_APPROVE');

INSERT INTO permissions (code, name, description)
SELECT 'SKILL_READ', 'Xem kỹ năng', 'Cho phép xem danh sách và chi tiết kỹ năng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SKILL_READ');

INSERT INTO permissions (code, name, description)
SELECT 'SKILL_CREATE', 'Tạo kỹ năng', 'Cho phép tạo kỹ năng và nhóm kỹ năng mới'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SKILL_CREATE');

INSERT INTO permissions (code, name, description)
SELECT 'SKILL_UPDATE', 'Cập nhật kỹ năng', 'Cho phép cập nhật thông tin kỹ năng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SKILL_UPDATE');

INSERT INTO permissions (code, name, description)
SELECT 'SKILL_MERGE', 'Gộp kỹ năng', 'Cho phép gộp các kỹ năng trùng lặp'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SKILL_MERGE');

INSERT INTO permissions (code, name, description)
SELECT 'SKILL_DEACTIVATE', 'Vô hiệu hóa kỹ năng', 'Cho phép vô hiệu hóa kỹ năng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'SKILL_DEACTIVATE');

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

-- Cấp quyền cho VT-06 (Quản trị viên)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VT-06' AND p.code IN ('SKILL_READ', 'SKILL_CREATE', 'SKILL_UPDATE', 'SKILL_MERGE', 'SKILL_DEACTIVATE')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);