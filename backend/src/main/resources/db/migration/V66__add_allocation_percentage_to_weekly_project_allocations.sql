-- V66: Add allocation_percentage to weekly_project_allocations for NCL-06-CN-007
ALTER TABLE weekly_project_allocations 
ADD COLUMN allocation_percentage DECIMAL(5, 2) NULL;

ALTER TABLE weekly_project_allocations 
ADD CONSTRAINT chk_wpa_allocation_percentage 
CHECK (allocation_percentage IS NULL OR (allocation_percentage >= 0.00 AND allocation_percentage <= 100.00));
