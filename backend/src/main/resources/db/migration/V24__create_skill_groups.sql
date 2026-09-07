-- V24__create_skill_groups.sql

-- =========================================================
-- 1. Create skill_groups table
-- =========================================================

CREATE TABLE IF NOT EXISTS skill_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_skill_groups_name UNIQUE (name)
);


-- =========================================================
-- 2. Create default skill group
-- =========================================================

INSERT INTO skill_groups (
    name,
    description,
    status
)
SELECT
    'General',
    'Default skill group',
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM skill_groups
    WHERE name = 'General'
);


-- =========================================================
-- 3. Add group_id to existing skills table and set default
-- =========================================================

ALTER TABLE skills
    ADD COLUMN group_id BIGINT NOT NULL DEFAULT 1;


-- =========================================================
-- 4. Add foreign key
-- =========================================================

ALTER TABLE skills
    ADD CONSTRAINT fk_skills_group
    FOREIGN KEY (group_id)
    REFERENCES skill_groups(id);