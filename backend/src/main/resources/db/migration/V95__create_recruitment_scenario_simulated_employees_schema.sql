-- ============================================================
-- FLYWAY MIGRATION V95: CREATE RECRUITMENT SCENARIO SIMULATED EMPLOYEES SCHEMA
-- Epic: NCL-08 (Mô phỏng kịch bản nhận thêm dự án)
-- Story: NCL-08-CN-005 (Kịch bản tuyển thêm nhân sự - QTN-14)
-- ============================================================

-- 1. Bổ sung Permissions mới cho Kịch bản tuyển thêm nhân sự
INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_RECRUITMENT_SCENARIO_MANAGE', 'Quản lý kịch bản tuyển thêm nhân sự', 'Cho phép Quản lý nguồn lực (VT-03) thêm nhân sự giả định và chạy lại kịch bản mô phỏng theo QTN-14'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RESOURCE_RECRUITMENT_SCENARIO_MANAGE');

INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_RECRUITMENT_SCENARIO_READ', 'Xem kịch bản tuyển thêm nhân sự', 'Cho phép xem kịch bản tuyển dụng mô phỏng'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'RESOURCE_RECRUITMENT_SCENARIO_READ');

-- 2. Gán quyền cho VT-03 (Quản lý nguồn lực) và VT-06 (Admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('VT-03', 'VT-06')
  AND p.code IN ('RESOURCE_RECRUITMENT_SCENARIO_MANAGE', 'RESOURCE_RECRUITMENT_SCENARIO_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. Tạo bảng khung tối thiểu simulation_scenarios nếu chưa có (không can thiệp logic của CN-001)
CREATE TABLE IF NOT EXISTS simulation_scenarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_sim_scenario_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 4. Tạo bảng kịch bản nhân sự giả định (tuân thủ tuyệt đối QTN-14 - hoàn toàn tách biệt với employees thật)
CREATE TABLE IF NOT EXISTS scenario_simulated_employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    candidate_name VARCHAR(150) NOT NULL,
    project_role_id BIGINT NOT NULL,
    primary_skill_id BIGINT NULL,
    standard_hours_per_week DECIMAL(5,2) NOT NULL DEFAULT 40.00,
    weeks_count INT NOT NULL DEFAULT 4,
    notes TEXT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    INDEX idx_sim_emp_scenario (scenario_id),
    INDEX idx_sim_emp_role (project_role_id),

    CONSTRAINT fk_sim_emp_scenario FOREIGN KEY (scenario_id) REFERENCES simulation_scenarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_sim_emp_role FOREIGN KEY (project_role_id) REFERENCES project_roles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sim_emp_skill FOREIGN KEY (primary_skill_id) REFERENCES skills(id) ON DELETE SET NULL,
    CONSTRAINT fk_sim_emp_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_sim_emp_hours CHECK (standard_hours_per_week > 0 AND standard_hours_per_week <= 80),
    CONSTRAINT chk_sim_emp_weeks CHECK (weeks_count > 0 AND weeks_count <= 52)
);
