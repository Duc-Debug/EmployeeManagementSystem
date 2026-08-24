CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_code VARCHAR(50) NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    org_unit_id BIGINT NOT NULL,
    manager_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_projects_project_code UNIQUE (project_code),

    CONSTRAINT fk_projects_org_unit
        FOREIGN KEY (org_unit_id)
        REFERENCES org_units(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_projects_manager
        FOREIGN KEY (manager_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_projects_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_projects_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_projects_org_unit_id
    ON projects(org_unit_id);

CREATE INDEX idx_projects_manager_id
    ON projects(manager_id);

CREATE INDEX idx_projects_status
    ON projects(status);

CREATE TABLE IF NOT EXISTS project_members (
    project_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,

    PRIMARY KEY (project_id, employee_id),

    CONSTRAINT fk_project_members_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_project_members_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_project_members_project_id
    ON project_members(project_id);

CREATE INDEX idx_project_members_employee_id
    ON project_members(employee_id);
