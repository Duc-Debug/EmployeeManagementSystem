-- ====================================================================
-- FLYWAY MIGRATION V26: ADD REVIEW_NOTES TO EMPLOYEE_SKILLS (NCL-02-CN-006)
-- ====================================================================

ALTER TABLE employee_skills
    ADD COLUMN review_notes VARCHAR(500) NULL AFTER rejection_reason;
