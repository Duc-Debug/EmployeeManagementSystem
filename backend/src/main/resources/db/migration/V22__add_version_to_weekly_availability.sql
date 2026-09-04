-- ============================================================
-- FLYWAY MIGRATION V20: ADD VERSION TO WEEKLY AVAILABILITY FOR OPTIMISTIC LOCKING
-- ============================================================

ALTER TABLE employee_weekly_availabilities
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
