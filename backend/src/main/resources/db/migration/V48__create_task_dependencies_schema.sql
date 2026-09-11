-- ============================================================
-- FLYWAY MIGRATION V44: CREATE TASK DEPENDENCIES SCHEMA
-- Epic: NCL-04 (Quản lý Tiến độ & Công việc Dự án)
-- Story: NCL-04-CN-004 (Khai báo phụ thuộc giữa các công việc)
-- ============================================================

-- 1. Bổ sung cột start_date và due_date cho bảng tasks để phục vụ tính toán lịch trình (TC-01)
ALTER TABLE tasks ADD COLUMN start_date DATE NULL;
ALTER TABLE tasks ADD COLUMN due_date DATE NULL;

-- 2. Tạo bảng task_dependencies quản lý mối quan hệ phụ thuộc giữa các công việc (Finish-to-Start)
CREATE TABLE IF NOT EXISTS task_dependencies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    predecessor_id BIGINT NOT NULL,
    successor_id BIGINT NOT NULL,
    dependency_type VARCHAR(20) NOT NULL DEFAULT 'FINISH_TO_START',
    lag_days INT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_dep_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_dep_predecessor
        FOREIGN KEY (predecessor_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_dep_successor
        FOREIGN KEY (successor_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_dep_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_task_dep_pred_succ
        UNIQUE (predecessor_id, successor_id),

    CONSTRAINT chk_task_dep_no_self
        CHECK (predecessor_id <> successor_id),

    CONSTRAINT chk_task_dep_type
        CHECK (dependency_type IN ('FINISH_TO_START', 'START_TO_START', 'FINISH_TO_FINISH', 'START_TO_FINISH'))
);

-- 3. Tạo các chỉ mục tối ưu hóa truy vấn đồ thị phụ thuộc
CREATE INDEX idx_task_dep_project_id 
    ON task_dependencies(project_id);

CREATE INDEX idx_task_dep_pred_id 
    ON task_dependencies(predecessor_id);

CREATE INDEX idx_task_dep_succ_id 
    ON task_dependencies(successor_id);

-- 4. Thêm các quyền quản lý phụ thuộc công việc (TC-03)
INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_TASK_DEPENDENCY_MANAGE', 'Quản lý phụ thuộc công việc', 'Cho phép khai báo và xóa mối quan hệ phụ thuộc giữa các công việc dự án'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_TASK_DEPENDENCY_MANAGE'
);

INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_TASK_DEPENDENCY_READ', 'Xem phụ thuộc công việc', 'Cho phép xem sơ đồ và mối quan hệ phụ thuộc công việc dự án'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_TASK_DEPENDENCY_READ'
);

-- 5. Gán quyền cho vai trò PM (VT-02) và Quản trị viên (VT-06)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('PROJECT_TASK_DEPENDENCY_MANAGE', 'PROJECT_TASK_DEPENDENCY_READ')
WHERE r.code IN ('VT-02', 'VT-06')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp 
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
