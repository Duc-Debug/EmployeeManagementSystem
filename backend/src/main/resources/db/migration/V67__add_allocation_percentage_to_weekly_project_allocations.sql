-- ============================================================
-- FLYWAY MIGRATION V67: ADD ALLOCATION PERCENTAGE TO WEEKLY PROJECT ALLOCATIONS
-- Epic: NCL-06 (Phan bo nguon luc theo tuan)
-- Story: NCL-06-CN-007 (Phan bo theo phan tram gio kha dụng)
-- ============================================================

ALTER TABLE weekly_project_allocations 
ADD COLUMN allocation_percentage DECIMAL(5, 2) NULL;

ALTER TABLE weekly_project_allocations 
ADD CONSTRAINT chk_wpa_allocation_percentage 
CHECK (allocation_percentage IS NULL OR (allocation_percentage >= 0.00 AND allocation_percentage <= 100.00));
