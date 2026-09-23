-- Persist reminder outcomes so invalid data cannot permanently block the pending queue.
ALTER TABLE timesheets ADD COLUMN reminder_status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
ALTER TABLE timesheets ADD COLUMN reminder_attempt_count INT NOT NULL DEFAULT 0;
ALTER TABLE timesheets ADD COLUMN last_reminder_error TEXT NULL;
ALTER TABLE timesheets ADD COLUMN next_reminder_at TIMESTAMP NULL;

UPDATE timesheets
SET reminder_status = 'SENT', reminder_attempt_count = 1
WHERE reminded_at IS NOT NULL;

CREATE INDEX idx_timesheets_reminder_queue
    ON timesheets (status, reminder_status, next_reminder_at, week_start_date, id);
