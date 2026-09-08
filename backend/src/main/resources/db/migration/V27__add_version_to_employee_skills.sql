-- ====================================================================
-- FLYWAY MIGRATION V27: ADD VERSION TO EMPLOYEE_SKILLS FOR OPTIMISTIC LOCKING
-- ====================================================================

ALTER TABLE employee_skills
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

