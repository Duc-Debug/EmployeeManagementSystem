-- ============================================================
-- FLYWAY MIGRATION V61: ADD OVERLOAD TRACKING FIELDS TO WEEKLY ALLOCATIONS
-- Epic: NCL-06 (Phan bo nguon luc theo tuan)
-- Story: NCL-06-CN-003 (Phat hien qua tai khi phan bo - QTN-11)
-- ============================================================

ALTER TABLE weekly_project_allocations
ADD COLUMN is_overloaded BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN overload_reason TEXT NULL,
ADD COLUMN overload_approved_by BIGINT NULL,
ADD COLUMN overload_approved_at TIMESTAMP NULL;

ALTER TABLE weekly_project_allocations
ADD CONSTRAINT fk_wpa_overload_approver
    FOREIGN KEY (overload_approved_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE weekly_project_allocations
ADD CONSTRAINT chk_overload_requires_reason
    CHECK (is_overloaded = FALSE OR (is_overloaded = TRUE AND overload_reason IS NOT NULL AND CHAR_LENGTH(TRIM(overload_reason)) > 0));

-- Index to optimize querying employee allocations for a specific week
CREATE INDEX idx_wpa_employee_year_week 
ON weekly_project_allocations (employee_id, year_number, week_number);
