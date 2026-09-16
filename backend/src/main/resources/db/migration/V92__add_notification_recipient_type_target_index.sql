-- ============================================================
-- FLYWAY MIGRATION V92: ADD NOTIFICATION RECIPIENT TYPE TARGET INDEX
-- Epic: NCL-07 (Cảnh báo xung đột lịch và đề xuất thay thế)
-- Story: NCL-07-CN-003 (Thông báo khi phân bổ thay đổi)
-- ============================================================

CREATE INDEX idx_notifications_recipient_type_target ON notifications(recipient_id, type, target_id);
