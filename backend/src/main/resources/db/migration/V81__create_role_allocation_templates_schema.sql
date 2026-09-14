-- V81: Create project role allocation templates and items schema

CREATE TABLE IF NOT EXISTS project_role_allocation_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    source_project_id BIGINT NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_role_template_source_project FOREIGN KEY (source_project_id) REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT fk_role_template_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS project_role_allocation_template_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    hours_per_week DECIMAL(8, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_template_items_template FOREIGN KEY (template_id) REFERENCES project_role_allocation_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_template_items_role FOREIGN KEY (role_id) REFERENCES project_roles(id) ON DELETE RESTRICT,
    CONSTRAINT uq_template_role UNIQUE (template_id, role_id),
    CONSTRAINT chk_template_item_hours CHECK (hours_per_week > 0)
);

CREATE INDEX idx_role_template_source_project ON project_role_allocation_templates(source_project_id);
CREATE INDEX idx_role_template_items_template ON project_role_allocation_template_items(template_id);
CREATE INDEX idx_role_template_items_role ON project_role_allocation_template_items(role_id);

