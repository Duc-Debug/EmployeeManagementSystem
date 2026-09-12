-- NCL-10-CN-005: a project role and a skill are different business concepts.
-- Do not infer a relation from matching labels or from a skill group.
CREATE TABLE IF NOT EXISTS project_role_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    required_level INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_role_skills_role FOREIGN KEY (role_id) REFERENCES project_roles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_project_role_skills_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE RESTRICT,
    CONSTRAINT uk_project_role_skills_role_skill UNIQUE (role_id, skill_id)
);

CREATE INDEX idx_project_role_skills_role_status ON project_role_skills (role_id, status);
