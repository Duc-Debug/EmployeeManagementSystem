-- 1. Bảng Nhóm Kỹ Năng
CREATE TABLE IF NOT EXISTS skill_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_skill_groups_name UNIQUE (name)
);

-- 2. Bảng Kỹ Năng
CREATE TABLE IF NOT EXISTS skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, MERGED
    merged_into_skill_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_skills_name UNIQUE (name),
    CONSTRAINT fk_skills_group FOREIGN KEY (group_id) REFERENCES skill_groups(id),
    CONSTRAINT fk_skills_merged_into FOREIGN KEY (merged_into_skill_id) REFERENCES skills(id)
);

-- 3. Bảng Trung Gian Employee - Skill
CREATE TABLE IF NOT EXISTS employee_skills (
    employee_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (employee_id, skill_id),
    CONSTRAINT fk_es_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_es_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

-- 4. Thêm Permissions vào hệ thống
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

-- 5. Gán toàn quyền quản trị kỹ năng cho Role Quản trị viên (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'SKILL_READ',
    'SKILL_CREATE',
    'SKILL_UPDATE',
    'SKILL_MERGE',
    'SKILL_DEACTIVATE'
)
WHERE r.code = 'VT-06'
AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
);