-- ============================================================
-- FLYWAY MIGRATION V83: ADD ALLOCATION NOTIFICATION INDEXES
-- Epic: NCL-07 (Cảnh báo xung đột lịch và đề xuất thay thế)
-- Story: NCL-07-CN-003 (Thông báo khi phân bổ thay đổi)
-- ============================================================

CREATE INDEX idx_notifications_type_target ON notifications(type, target_type, target_id);
