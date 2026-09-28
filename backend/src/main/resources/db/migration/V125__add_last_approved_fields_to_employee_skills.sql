-- ====================================================================
-- FLYWAY MIGRATION V125: EMPLOYEE SKILLS APPROVAL HISTORY & PENDING FIELDS
-- ====================================================================

ALTER TABLE employee_skills ADD COLUMN last_approved_proficiency_level INT NULL;
ALTER TABLE employee_skills ADD COLUMN last_approved_years_of_experience DECIMAL(4, 1) NULL;
ALTER TABLE employee_skills ADD COLUMN pending_proficiency_level INT NULL;
ALTER TABLE employee_skills ADD COLUMN pending_years_of_experience DECIMAL(4, 1) NULL;

UPDATE employee_skills
SET last_approved_proficiency_level = proficiency_level,
    last_approved_years_of_experience = years_of_experience
WHERE status = 'APPROVED' AND last_approved_proficiency_level IS NULL;

