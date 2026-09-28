-- ============================================================
-- FLYWAY MIGRATION V128: FIX DUPLICATE SCHEDULE CONFLICT INDEX
-- Epic: NCL-07 (Quản lý & Theo dõi Phân bổ)
-- Clean up redundant unique index uk_conflict_emp_year_week_type
-- on schedule_conflict_warnings, keeping uk_schedule_conflict_existing
-- ============================================================

-- Drop redundant duplicate unique index/constraint created in V76 if present
-- (V93 established canonical uk_schedule_conflict_existing on the same columns)
ALTER TABLE schedule_conflict_warnings DROP CONSTRAINT IF EXISTS uk_conflict_emp_year_week_type;
