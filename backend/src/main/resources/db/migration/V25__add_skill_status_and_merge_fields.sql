-- ====================================================================
-- FLYWAY MIGRATION V25: ADD STATUS, MERGED_INTO AND UPDATED_AT TO SKILLS
-- ====================================================================

-- 1. Bổ sung cột status (Mặc định 'ACTIVE' cho các bản ghi hiện có)
ALTER TABLE skills 
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- 2. Bổ sung cột merged_into_skill_id (NULL cho các bản ghi chưa bị gộp)
ALTER TABLE skills 
    ADD COLUMN merged_into_skill_id BIGINT NULL;

-- 3. Bổ sung cột updated_at
ALTER TABLE skills 
    ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 4. Bổ sung Foreign Key tự tham chiếu cho merged_into_skill_id
ALTER TABLE skills 
    ADD CONSTRAINT fk_skills_merged_into 
    FOREIGN KEY (merged_into_skill_id) 
    REFERENCES skills(id);
