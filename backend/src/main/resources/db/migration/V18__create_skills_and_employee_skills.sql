-- 1. Bảng danh mục kỹ năng chuẩn
CREATE TABLE skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NULL,
    description TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng kỹ năng nhân viên (TC-01, TC-02)
CREATE TABLE employee_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency_level INT NOT NULL,
    years_of_experience DECIMAL(4,1) NOT NULL DEFAULT 0.0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    approved_by BIGINT NULL,
    approved_at DATETIME NULL,
    rejection_reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_emp_skill_proficiency CHECK (proficiency_level BETWEEN 1 AND 5),
    CONSTRAINT fk_emp_skill_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_emp_skill_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
    -- Ràng buộc chống dữ liệu trùng lặp (TC-02)
    CONSTRAINT uq_employee_skill UNIQUE (employee_id, skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Nạp dữ liệu danh mục kỹ năng mẫu
INSERT INTO skills (code, name, category, description) VALUES
('JAVA', 'Java', 'Backend', 'Ngôn ngữ lập trình Java'),
('SPRING_BOOT', 'Spring Boot', 'Backend', 'Framework Java Spring Boot'),
('REACT', 'React.js', 'Frontend', 'Thư viện UI React.js'),
('TYPESCRIPT', 'TypeScript', 'Frontend', 'Ngôn ngữ TypeScript'),
('MYSQL', 'MySQL Database', 'Database', 'Hệ quản trị CSDL MySQL'),
('DOCKER', 'Docker', 'DevOps', 'Công nghệ Containerization');

-- 4. Thêm các quyền mới & cấp cho VT-04 (Nhân viên) và VT-03 (Quản lý nguồn lực)
INSERT INTO permissions (code, name, description) VALUES
('EMPLOYEE_SKILL_READ', 'Xem kỹ năng nhân sự', 'Quyền xem hồ sơ kỹ năng'),
('EMPLOYEE_SKILL_DECLARE', 'Khai báo kỹ năng cá nhân', 'Quyền khai báo kỹ năng dành cho nhân viên'),
('EMPLOYEE_SKILL_APPROVE', 'Phê duyệt kỹ năng nhân sự', 'Quyền duyệt kỹ năng dành cho RM');

-- Cấp quyền khai báo cho VT-04 (Nhân viên chuyên môn)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VT-04' AND p.code IN ('EMPLOYEE_SKILL_READ', 'EMPLOYEE_SKILL_DECLARE');

-- Cấp quyền duyệt cho VT-03 (Quản lý nguồn lực)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VT-03' AND p.code IN ('EMPLOYEE_SKILL_READ', 'EMPLOYEE_SKILL_APPROVE');