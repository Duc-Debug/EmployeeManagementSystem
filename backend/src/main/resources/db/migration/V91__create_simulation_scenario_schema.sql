-- ============================================================
-- FLYWAY MIGRATION V88: CREATE SIMULATION SCENARIO SCHEMA
-- Epic: NCL-08 (Mô phỏng kịch bản nhận thêm dự án)
-- Story: NCL-08-CN-001 (Tạo kịch bản mô phỏng nhận thêm dự án - QTN-14)
-- ============================================================

-- 1. Tạo bảng resource_scenarios quản lý kịch bản mô phỏng sandbox
CREATE TABLE IF NOT EXISTS resource_scenarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    org_unit_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'draft',
    from_year INT NOT NULL,
    from_week INT NOT NULL,
    duration_weeks INT NOT NULL DEFAULT 8,
    base_snapshot_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_scenario_code UNIQUE (code),
    CONSTRAINT chk_scenario_status CHECK (status IN ('draft', 'applied', 'discarded')),
    CONSTRAINT chk_scenario_weeks CHECK (from_week BETWEEN 1 AND 53 AND duration_weeks BETWEEN 1 AND 16),
    CONSTRAINT fk_scenario_org_unit FOREIGN KEY (org_unit_id) REFERENCES org_units(id) ON DELETE RESTRICT,
    CONSTRAINT fk_scenario_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX idx_scenario_org_unit ON resource_scenarios (org_unit_id, status);
CREATE INDEX idx_scenario_created_by ON resource_scenarios (created_by);

-- 2. Tạo bảng scenario_demands lưu trữ các nhu cầu nhân sự giả định
CREATE TABLE IF NOT EXISTS scenario_demands (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    demand_name VARCHAR(255) NOT NULL,
    headcount INT NOT NULL,
    start_year INT NOT NULL,
    start_week INT NOT NULL,
    end_year INT NOT NULL,
    end_week INT NOT NULL,
    hours_per_week_per_person DECIMAL(5, 2) NOT NULL,
    skill_requirement VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_demand_scenario FOREIGN KEY (scenario_id) REFERENCES resource_scenarios(id) ON DELETE CASCADE,
    CONSTRAINT chk_demand_headcount CHECK (headcount > 0),
    CONSTRAINT chk_demand_hours CHECK (hours_per_week_per_person >= 0),
    CONSTRAINT chk_demand_start_week CHECK (start_week BETWEEN 1 AND 53),
    CONSTRAINT chk_demand_end_week CHECK (end_week BETWEEN 1 AND 53),
    CONSTRAINT chk_demand_year_week CHECK (
        start_year < end_year OR (start_year = end_year AND start_week <= end_week)
    )
);

CREATE INDEX idx_demand_scenario_id ON scenario_demands (scenario_id);

-- 3. Tạo bảng scenario_allocation_snapshot lưu bản chụp phân bổ & khả dụng nhân sự tại thời điểm tạo kịch bản
CREATE TABLE IF NOT EXISTS scenario_allocation_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    year_number INT NOT NULL,
    week_number INT NOT NULL,
    allocated_hours DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    available_hours DECIMAL(5, 2) NOT NULL DEFAULT 0.00,

    CONSTRAINT fk_sas_scenario FOREIGN KEY (scenario_id) REFERENCES resource_scenarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_sas_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT,
    CONSTRAINT uk_sas_scenario_emp_week UNIQUE (scenario_id, employee_id, year_number, week_number)
);

CREATE INDEX idx_sas_scenario_id ON scenario_allocation_snapshot (scenario_id);
CREATE INDEX idx_sas_emp_week ON scenario_allocation_snapshot (employee_id, year_number, week_number);

-- 4. Bổ sung Permissions cho NCL-08
INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_SCENARIO_MANAGE', 'Quản lý kịch bản mô phỏng', 'Cho phép Quản lý nguồn lực (VT-03) tạo, chỉnh sửa nhu cầu kịch bản mô phỏng nhận thêm dự án theo QTN-14'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RESOURCE_SCENARIO_MANAGE');

INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_SCENARIO_READ', 'Xem kịch bản mô phỏng', 'Cho phép xem kịch bản mô phỏng và kết quả mô phỏng năng lực theo QTN-14'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RESOURCE_SCENARIO_READ');

-- Gán quyền RESOURCE_SCENARIO_MANAGE duy nhất cho VT-03 (Quản lý nguồn lực)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'RESOURCE_SCENARIO_MANAGE'
WHERE r.code = 'VT-03'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- Gán quyền RESOURCE_SCENARIO_READ cho VT-01 (Ban giám đốc) và VT-03 (Quản lý nguồn lực)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'RESOURCE_SCENARIO_READ'
  AND r.code IN ('VT-01', 'VT-03')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
