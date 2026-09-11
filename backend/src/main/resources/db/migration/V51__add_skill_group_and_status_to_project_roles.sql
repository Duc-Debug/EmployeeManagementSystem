-- ============================================================
-- FLYWAY MIGRATION V51: ADD SKILL GROUP AND STATUS TO PROJECT ROLES
-- Story: NCL-12-CN-001 (Quản lý danh mục vai trò chuyên môn)
-- ============================================================

-- 1. Đảm bảo tồn tại nhóm kỹ năng mặc định 'General' nếu chưa có
INSERT INTO skill_groups (name, description, status)
SELECT 'General', 'Default skill group', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM skill_groups WHERE name = 'General');

-- 2. Bổ sung cột skill_group_id (dạng NULL an toàn), status và updated_at cho project_roles
ALTER TABLE project_roles
    ADD COLUMN skill_group_id BIGINT NULL;

ALTER TABLE project_roles
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE project_roles
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 3. Backfill động nhóm kỹ năng hợp lệ (tìm theo tên 'General' tất định)
UPDATE project_roles
SET skill_group_id = (SELECT id FROM skill_groups WHERE name = 'General' LIMIT 1)
WHERE skill_group_id IS NULL;

-- 4. Chuyển cột skill_group_id sang NOT NULL sau khi đã gán dữ liệu đầy đủ
ALTER TABLE project_roles
    MODIFY COLUMN skill_group_id BIGINT NOT NULL;

-- 5. Ràng buộc Khóa ngoại tới bảng skill_groups
ALTER TABLE project_roles
    ADD CONSTRAINT fk_project_roles_skill_group
    FOREIGN KEY (skill_group_id) REFERENCES skill_groups(id) ON DELETE RESTRICT;

CREATE INDEX idx_project_roles_skill_group_id
    ON project_roles(skill_group_id);

-- 6. Ràng buộc UNIQUE cho tên vai trò chuyên môn (chống race condition)
ALTER TABLE project_roles
    ADD CONSTRAINT uk_project_roles_name UNIQUE (name);

-- 3. Bổ sung các quyền quản trị danh mục vai trò chuyên môn
INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_ROLE_READ', 'Xem danh mục vai trò chuyên môn', 'Cho phép xem danh mục vai trò chuyên môn dùng chung'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PROJECT_ROLE_READ');

INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_ROLE_MANAGE', 'Quản lý danh mục vai trò chuyên môn', 'Cho phép tạo, cập nhật và ngừng sử dụng vai trò chuyên môn'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'PROJECT_ROLE_MANAGE');

-- 4. Cấp quyền PROJECT_ROLE_READ cho tất cả 6 vai trò chính thức (VT-01 -> VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'PROJECT_ROLE_READ'
  AND r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 5. Cấp quyền PROJECT_ROLE_MANAGE cho duy nhất Quản trị viên (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'PROJECT_ROLE_MANAGE'
  AND r.code = 'VT-06'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );