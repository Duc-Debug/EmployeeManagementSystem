-- ============================================================
-- FLYWAY MIGRATION V68: REPAIR WEEKLY ALLOCATION OVERLOAD SCHEMA
-- Epic: NCL-06 (Phan bo nguon luc theo tuan)
-- Story: NCL-06-CN-003 (Phat hien qua tai khi phan bo - QTN-11)
-- ============================================================

-- Ensure overload tracking columns exist on MySQL and H2
ALTER TABLE weekly_project_allocations ADD COLUMN IF NOT EXISTS is_overloaded BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE weekly_project_allocations ADD COLUMN IF NOT EXISTS overload_reason VARCHAR(1000) NULL;
ALTER TABLE weekly_project_allocations ADD COLUMN IF NOT EXISTS overload_approved_by BIGINT NULL;
ALTER TABLE weekly_project_allocations ADD COLUMN IF NOT EXISTS overload_approved_at TIMESTAMP NULL;
