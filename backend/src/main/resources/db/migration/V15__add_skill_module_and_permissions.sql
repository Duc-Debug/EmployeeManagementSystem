-- =============================================================================
-- 1. SKILL GROUPS
-- =============================================================================
CREATE TABLE skill_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_skill_groups_name UNIQUE (name)
);

-- =============================================================================
-- 2. SKILLS
-- =============================================================================
CREATE TABLE skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, MERGED
    merged_into_skill_id BIGINT NULL,             -- Trỏ về skill đích nếu bị merge
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_skills_name UNIQUE (name),
    CONSTRAINT fk_skills_group FOREIGN KEY (group_id) REFERENCES skill_groups(id),
    CONSTRAINT fk_skills_merged_into FOREIGN KEY (merged_into_skill_id) REFERENCES skills(id)
);

-- =============================================================================
-- 3. EMPLOYEE_SKILLS (Bảng trung gian)
-- =============================================================================
CREATE TABLE employee_skills (
    employee_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (employee_id, skill_id),
    -- Lưu ý: Tên bảng employees có thể khác tùy project hiện tại (có thể là `employee` hoặc `users`)
    CONSTRAINT fk_es_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_es_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

-- =============================================================================
-- 4. THÊM PERMISSIONS CHO MODULE SKILL
-- =============================================================================
INSERT INTO permissions (code, description, module) VALUES 
('SKILL_READ', 'Xem danh sách và chi tiết kỹ năng', 'SKILL'),
('SKILL_CREATE', 'Tạo kỹ năng và nhóm kỹ năng mới', 'SKILL'),
('SKILL_UPDATE', 'Cập nhật thông tin kỹ năng', 'SKILL'),
('SKILL_MERGE', 'Gộp các kỹ năng trùng lặp', 'SKILL'),
('SKILL_DEACTIVATE', 'Vô hiệu hóa kỹ năng', 'SKILL');

-- =============================================================================
-- 5. GÁN PERMISSION CHO ROLE VT-06
-- =============================================================================
INSERT INTO role_permissions (role_id, permission_code)
SELECT id, 'SKILL_READ' FROM roles WHERE code = 'VT-06'
UNION ALL
SELECT id, 'SKILL_CREATE' FROM roles WHERE code = 'VT-06'
UNION ALL
SELECT id, 'SKILL_UPDATE' FROM roles WHERE code = 'VT-06'
UNION ALL
SELECT id, 'SKILL_MERGE' FROM roles WHERE code = 'VT-06'
UNION ALL
SELECT id, 'SKILL_DEACTIVATE' FROM roles WHERE code = 'VT-06';