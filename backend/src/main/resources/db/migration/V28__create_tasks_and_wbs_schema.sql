-- ============================================================
-- FLYWAY MIGRATION V28: CREATE TASKS & WBS SCHEMA
-- Epic: NCL-03 (Dự án và cây công việc)
-- Story: NCL-03-CN-002 (Chia hạng mục và công việc của dự án)
-- ============================================================

-- 1. Bổ sung trạng thái 'CLOSED' và bộ đếm task_seq_counter cho bảng projects
ALTER TABLE projects DROP CONSTRAINT chk_projects_status;
ALTER TABLE projects 
    ADD CONSTRAINT chk_projects_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED'));

ALTER TABLE projects 
    ADD COLUMN task_seq_counter INT NOT NULL DEFAULT 0;

-- 2. Tạo bảng tasks hỗ trợ cấu trúc WBS phân cấp nhiều tầng (AC-01 / TC-01)
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    task_code VARCHAR(50) NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    task_type VARCHAR(20) NOT NULL DEFAULT 'TASK',
    assignee_id BIGINT NULL,
    estimated_hours DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    actual_hours DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'TODO',
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_tasks_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_tasks_parent
        FOREIGN KEY (parent_id)
        REFERENCES tasks(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_tasks_assignee
        FOREIGN KEY (assignee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_tasks_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_tasks_project_task_code
        UNIQUE (project_id, task_code),

    CONSTRAINT chk_tasks_type
        CHECK (task_type IN ('CATEGORY', 'TASK')),

    CONSTRAINT chk_tasks_status
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED')),

    CONSTRAINT chk_tasks_estimated_hours
        CHECK (estimated_hours >= 0),

    CONSTRAINT chk_tasks_actual_hours
        CHECK (actual_hours >= 0)
);

-- 3. Tạo các chỉ mục tối ưu hóa truy vấn dựng cây WBS và tìm kiếm
CREATE INDEX idx_tasks_project_id 
    ON tasks(project_id);

CREATE INDEX idx_tasks_parent_id 
    ON tasks(parent_id);

CREATE INDEX idx_tasks_assignee_id 
    ON tasks(assignee_id);

CREATE INDEX idx_tasks_project_parent_sort 
    ON tasks(project_id, parent_id, sort_order);

-- 4. Bổ sung quyền quản trị cây công việc WBS cho vai trò Quản lý dự án (VT-02) (AC-03 / TC-03)
INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_WBS_MANAGE', 'Quản lý cây công việc', 'Cho phép tạo và chia hạng mục, công việc trong dự án'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_WBS_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'PROJECT_WBS_MANAGE'
WHERE r.code = 'VT-02'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);