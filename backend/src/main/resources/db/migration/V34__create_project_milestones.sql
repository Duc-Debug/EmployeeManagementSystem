-- ============================================================
-- FLYWAY MIGRATION V34: CREATE PROJECT MILESTONES SCHEMA
-- Epic: NCL-03 (Dự án và cây công việc)
-- Story: NCL-03-CN-006 (Quản lý mốc tiến độ của dự án)
-- ============================================================

-- 1. Tạo bảng project_milestones lưu trữ các mốc tiến độ của dự án
CREATE TABLE IF NOT EXISTS project_milestones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    planned_date DATE NOT NULL,
    actual_date DATE NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_milestones_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_milestones_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT uk_milestones_project_name
        UNIQUE (project_id, name)
);

-- 2. Tạo bảng milestone_tasks liên kết giữa mốc tiến độ và các công việc/hạng mục WBS
CREATE TABLE IF NOT EXISTS milestone_tasks (
    milestone_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,

    PRIMARY KEY (milestone_id, task_id),

    CONSTRAINT fk_mt_milestone
        FOREIGN KEY (milestone_id)
        REFERENCES project_milestones(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_mt_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE
);

-- 3. Tạo các chỉ mục tối ưu hóa truy vấn
CREATE INDEX idx_milestones_project_id 
    ON project_milestones(project_id);

CREATE INDEX idx_milestones_planned_date 
    ON project_milestones(planned_date);

CREATE INDEX idx_milestone_tasks_task_id 
    ON milestone_tasks(task_id);

-- 4. Bổ sung quyền quản lý mốc tiến độ cho vai trò Quản lý dự án (VT-02) (AC-03 / TC-03)
INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_MILESTONE_MANAGE', 'Quản lý mốc tiến độ', 'Cho phép khai báo và theo dõi mốc tiến độ dự án'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_MILESTONE_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'PROJECT_MILESTONE_MANAGE'
WHERE r.code IN ('VT-02', 'VT-06')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
