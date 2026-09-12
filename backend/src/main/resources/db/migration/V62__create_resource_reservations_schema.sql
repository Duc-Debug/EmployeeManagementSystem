-- ============================================================
-- FLYWAY MIGRATION V62: CREATE RESOURCE RESERVATIONS SCHEMA
-- Epic: NCL-06 (Phan bo nguon luc theo tuan)
-- Story: NCL-06-CN-005 (Giu cho nguon luc cho du an du kien - QTN-13)
-- ============================================================

-- 1. Cap nhat rang buoc trang thai cho bang projects de ho tro PLANNED va CANCELLED
ALTER TABLE projects DROP CONSTRAINT chk_projects_status;
ALTER TABLE projects 
    ADD CONSTRAINT chk_projects_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED', 'PLANNED', 'CANCELLED'));

-- 2. Tao bang resource_reservations luu thong tin giu cho nhan su theo tuan
CREATE TABLE IF NOT EXISTS resource_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    year_number INT NOT NULL,
    week_number INT NOT NULL,
    reserved_hours DECIMAL(5, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    converted_allocation_id BIGINT NULL,
    cancelled_reason VARCHAR(500) NULL,
    note VARCHAR(1000) NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_rr_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_rr_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_rr_converted_allocation
        FOREIGN KEY (converted_allocation_id)
        REFERENCES weekly_project_allocations(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_rr_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_rr_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_rr_reserved_hours
        CHECK (reserved_hours > 0.00 AND reserved_hours <= 168.00),

    CONSTRAINT chk_rr_status
        CHECK (status IN ('ACTIVE', 'CONVERTED', 'CANCELLED'))
);

-- 3. Tao chi muc toi uu truy van
CREATE INDEX idx_rr_employee_year_week
    ON resource_reservations(employee_id, year_number, week_number);

CREATE INDEX idx_rr_project_status
    ON resource_reservations(project_id, status);

CREATE INDEX idx_rr_status
    ON resource_reservations(status);

-- 4. Bo sung quyen giu cho nguon luc vao permissions
INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_RESERVATION_CREATE', 'Tạo giữ chỗ nguồn lực', 'Cho phép Quản lý dự án và Quản lý nguồn lực giữ chỗ nhân sự cho dự án dự kiến'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'RESOURCE_RESERVATION_CREATE'
);

INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_RESERVATION_MANAGE', 'Quản lý giữ chỗ nguồn lực', 'Cho phép Quản lý nguồn lực và Quản lý dự án điều phối và hủy giữ chỗ nhân sự'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'RESOURCE_RESERVATION_MANAGE'
);

-- 5. Phan quyen cho VT-02 (Quan ly du an) va VT-03 (Quan ly nguon luc)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('RESOURCE_RESERVATION_CREATE', 'RESOURCE_RESERVATION_MANAGE')
WHERE r.code IN ('VT-02', 'VT-03')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);
