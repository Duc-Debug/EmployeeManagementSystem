-- V86: Create timesheet_histories table for tracking timesheet events
CREATE TABLE IF NOT EXISTS timesheet_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timesheet_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    action_by BIGINT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ts_histories_timesheet FOREIGN KEY (timesheet_id) REFERENCES timesheets(id) ON DELETE CASCADE,
    CONSTRAINT fk_ts_histories_user FOREIGN KEY (action_by) REFERENCES users(id) ON DELETE SET NULL
);
