-- ============================================================
-- FLYWAY MIGRATION V71: ADD IN_REVIEW TO TASK STATUS
-- Epic: NCL-04 (Giao việc và theo dõi tiến độ)
-- Story: NCL-04-CN-006 (Bảng công việc theo cột trạng thái)
-- ============================================================

ALTER TABLE tasks DROP CONSTRAINT chk_tasks_status;
ALTER TABLE tasks 
    ADD CONSTRAINT chk_tasks_status 
    CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'CANCELLED'));
