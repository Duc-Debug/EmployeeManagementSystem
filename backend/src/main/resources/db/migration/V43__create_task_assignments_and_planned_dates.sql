-- ============================================================
-- FLYWAY MIGRATION V43: CREATE TASK ASSIGNMENTS & PLANNED DATES
-- UC: Phân công công việc cho nhân sự kèm ngày bắt đầu và kết thúc mong muốn
-- ============================================================

-- 1. Bổ sung planned_start_date và planned_end_date vào bảng tasks
ALTER TABLE tasks ADD COLUMN planned_start_date DATE NULL;
ALTER TABLE tasks ADD COLUMN planned_end_date DATE NULL;

-- 2. Tạo bảng task_assignments hỗ trợ giao việc cho một hoặc nhiều nhân sự
CREATE TABLE IF NOT EXISTS task_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_task_assignments_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_assignments_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_task_assignments_assigned_by
        FOREIGN KEY (assigned_by)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT uk_task_assignments_task_employee
        UNIQUE (task_id, employee_id)
);

CREATE INDEX idx_task_assignments_task ON task_assignments(task_id);
CREATE INDEX idx_task_assignments_employee ON task_assignments(employee_id);

-- 3. Backfill dữ liệu từ tasks.assignee_id sang task_assignments
INSERT INTO task_assignments (task_id, employee_id, assigned_at, assigned_by, is_primary)
SELECT t.id, t.assignee_id, CURRENT_TIMESTAMP, t.created_by, TRUE
FROM tasks t
WHERE t.assignee_id IS NOT NULL
ON DUPLICATE KEY UPDATE is_primary = TRUE;

