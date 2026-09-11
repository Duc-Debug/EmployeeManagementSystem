-- ============================================================
-- FLYWAY MIGRATION V45: ADD LAG DAYS CONSTRAINT TO TASK DEPENDENCIES
-- Epic: NCL-04 (Quản lý Tiến độ & Công việc Dự án)
-- Story: NCL-04-CN-004 (Khai báo phụ thuộc giữa các công việc)
-- ============================================================

ALTER TABLE task_dependencies 
    ADD CONSTRAINT chk_task_dep_lag_days CHECK (lag_days >= 0);
