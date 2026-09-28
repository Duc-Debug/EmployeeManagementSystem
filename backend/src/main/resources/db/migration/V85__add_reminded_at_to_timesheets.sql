-- V85: Add reminded_at to timesheets to prevent duplicate reminder notifications
ALTER TABLE timesheets
ADD COLUMN reminded_at TIMESTAMP NULL;
