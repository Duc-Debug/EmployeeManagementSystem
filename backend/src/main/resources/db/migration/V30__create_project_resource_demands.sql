-- ============================================================
-- FLYWAY MIGRATION V30: CREATE PROJECT RESOURCE DEMANDS SCHEMA
-- Epic: NCL-03 (Dự án và cây công việc)
-- Story: NCL-03-CN-007 (Ước lượng nhu cầu nhân sự theo vai trò)
-- ============================================================

-- 1. Tạo bảng project_resource_demands
CREATE TABLE IF NOT EXISTS project_resource_demands (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    year_number INT NOT NULL,
    week_number INT NOT NULL,
    required_hours DECIMAL(8, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_proj_res_demand_proj_role_week 
        UNIQUE (project_id, role_id, year_number, week_number),

    CONSTRAINT fk_proj_res_demands_project 
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE RESTRICT,

    CONSTRAINT fk_proj_res_demands_role 
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT,

    CONSTRAINT chk_proj_res_demands_hours 
        CHECK (required_hours > 0.00 AND required_hours <= 168.00),

    CONSTRAINT chk_proj_res_demands_week 
        CHECK (week_number >= 1 AND week_number <= 53)
);

-- 2. Đánh Index để tối ưu truy vấn
CREATE INDEX idx_proj_res_demands_project_id 
    ON project_resource_demands(project_id);

CREATE INDEX idx_proj_res_demands_role_id 
    ON project_resource_demands(role_id);

CREATE INDEX idx_proj_res_demands_lookup 
    ON project_resource_demands(project_id, role_id, year_number, week_number);

-- 3. Bổ sung Permissions cho hệ thống
INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_RESOURCE_DEMAND_ESTIMATE', 'Ước lượng nhu cầu nhân sự', 'Cho phép ước lượng giờ theo vai trò và tuần cho dự án'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_RESOURCE_DEMAND_ESTIMATE'
);

INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_RESOURCE_DEMAND_READ', 'Xem nhu cầu nhân sự dự án', 'Cho phép xem bảng tổng hợp nhu cầu nhân sự của dự án'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_RESOURCE_DEMAND_READ'
);

-- 4. Gán quyền cho VT-02 (Quản lý dự án - PM), VT-01 (Ban giám đốc), VT-06 (Quản trị viên)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('VT-02', 'VT-01', 'VT-06')
  AND p.code IN ('PROJECT_RESOURCE_DEMAND_ESTIMATE', 'PROJECT_RESOURCE_DEMAND_READ')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );