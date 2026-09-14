-- ============================================================
-- FLYWAY MIGRATION V71: ADD ALLOCATION ADJUSTMENT AND CHANGE LOGS
-- Epic: NCL-06 (Phân bổ nguồn lực theo tuần)
-- Story: NCL-06-CN-004 (Điều chỉnh phân bổ nguồn lực - QTN-15)
-- ============================================================

-- 1. Bổ sung các cột phục vụ điều chỉnh phân bổ và ghi chú lý do chênh lệch (TC-02)
ALTER TABLE weekly_project_allocations 
ADD COLUMN variance_note VARCHAR(1000) NULL;

ALTER TABLE weekly_project_allocations 
ADD COLUMN updated_by BIGINT NULL;

ALTER TABLE weekly_project_allocations 
ADD CONSTRAINT fk_wpa_updated_by 
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL;

-- 2. Tạo bảng allocation_change_logs phục vụ lưu vết lịch sử điều chỉnh phân bổ (QTN-15, TC-04)
CREATE TABLE IF NOT EXISTS allocation_change_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    allocation_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_value TEXT NOT NULL,
    new_value TEXT NOT NULL,
    changed_by BIGINT NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notified_pm_ids VARCHAR(255) NULL,
    CONSTRAINT fk_alloc_log_changed_by FOREIGN KEY (changed_by) REFERENCES users(id)
);

CREATE INDEX idx_alloc_log_allocation ON allocation_change_logs(allocation_id);
CREATE INDEX idx_alloc_log_changed_at ON allocation_change_logs(changed_at);
