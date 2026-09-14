-- ============================================================
-- FLYWAY MIGRATION V77: HUY DON NGHI PHEP DA DUYET
-- Epic: NCL-05 (Quan ly nghi phep)
-- Story: NCL-05-CN-007 (Huy don nghi phep da duyet)
-- ============================================================

-- 1. Bo sung cac cot luu ly do va thoi diem yeu cau huy don nghi phep da duyet
ALTER TABLE leave_requests ADD COLUMN cancellation_reason VARCHAR(500) NULL;
ALTER TABLE leave_requests ADD COLUMN cancellation_requested_at TIMESTAMP NULL;

-- 2. Tao chi muc ho tro tim kiem theo trang thai don nghi phep
CREATE INDEX idx_leave_requests_cancellation_lookup
    ON leave_requests (employee_id, status, start_date);
