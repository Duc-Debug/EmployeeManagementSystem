-- ============================================================
-- FLYWAY MIGRATION V72: CREATE ALLOCATION PLANNING PERIODS AND SNAPSHOTS SCHEMA
-- Epic: NCL-06 (Phan bo nguon luc theo tuan)
-- Story: NCL-06-CN-009 (Khoa ke hoach phan bo cua ky - QTN-18)
-- ============================================================

-- 1. Tao bang allocation_planning_periods luu thong tin ky ke hoach phan bo
CREATE TABLE IF NOT EXISTS allocation_planning_periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    period_type VARCHAR(30) NOT NULL DEFAULT 'QUARTER',
    year_number INT NOT NULL,
    start_week INT NOT NULL,
    end_week INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    locked_by BIGINT NULL,
    locked_at TIMESTAMP NULL,
    unlocked_by BIGINT NULL,
    unlocked_at TIMESTAMP NULL,
    unlock_reason VARCHAR(1000) NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_app_period_type CHECK (period_type IN ('QUARTER', 'MONTH', 'CUSTOM')),
    CONSTRAINT chk_app_status CHECK (status IN ('OPEN', 'LOCKED')),
    CONSTRAINT chk_app_weeks CHECK (start_week BETWEEN 1 AND 53 AND end_week BETWEEN 1 AND 53 AND start_week <= end_week),
    CONSTRAINT fk_app_locked_by FOREIGN KEY (locked_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_app_unlocked_by FOREIGN KEY (unlocked_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_app_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
);

-- Index ho tro tim kiem ky theo nam va khoang tuan cho QTN-18
CREATE INDEX idx_app_year_weeks ON allocation_planning_periods (year_number, start_week, end_week, status);

-- 2. Tao bang allocation_plan_snapshots luu ban chup tong the cua ky tai thoi diem khoa
CREATE TABLE IF NOT EXISTS allocation_plan_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period_id BIGINT NOT NULL,
    snapshot_version INT NOT NULL DEFAULT 1,
    total_allocations INT NOT NULL DEFAULT 0,
    total_allocated_hours DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_aps_period FOREIGN KEY (period_id) REFERENCES allocation_planning_periods(id) ON DELETE CASCADE,
    CONSTRAINT fk_aps_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uk_aps_period_version UNIQUE (period_id, snapshot_version)
);

CREATE INDEX idx_aps_period_id ON allocation_plan_snapshots (period_id);

-- 3. Tao bang allocation_plan_snapshot_items luu chi tiet tung dong phan bo trong ban chup
CREATE TABLE IF NOT EXISTS allocation_plan_snapshot_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_id BIGINT NOT NULL,
    original_allocation_id BIGINT NULL,
    employee_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    year_number INT NOT NULL,
    week_number INT NOT NULL,
    allocated_hours DECIMAL(5, 2) NOT NULL,
    allocation_percentage DECIMAL(5, 2) NULL,
    is_overloaded BOOLEAN NOT NULL DEFAULT FALSE,
    overload_reason VARCHAR(1000) NULL,

    CONSTRAINT fk_apsi_snapshot FOREIGN KEY (snapshot_id) REFERENCES allocation_plan_snapshots(id) ON DELETE CASCADE,
    CONSTRAINT fk_apsi_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT,
    CONSTRAINT fk_apsi_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE RESTRICT
);

CREATE INDEX idx_apsi_snapshot_id ON allocation_plan_snapshot_items (snapshot_id);
CREATE INDEX idx_apsi_emp_proj ON allocation_plan_snapshot_items (employee_id, project_id);

-- 4. Cap quyen he thong cho Quan ly nguon luc (VT-03) theo QTN-18
INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_ALLOCATION_LOCK', 'Khóa kế hoạch phân bổ của kỳ', 'Cho phép Quản lý nguồn lực khóa và mở lại kế hoạch phân bổ theo QTN-18'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'RESOURCE_ALLOCATION_LOCK'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'RESOURCE_ALLOCATION_LOCK'
WHERE r.code = 'VT-03'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
