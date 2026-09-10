-- ============================================================
-- FLYWAY MIGRATION V32: ADD TASK BUDGET HOURS
-- Epic: NCL-03 (Dự án và cây công việc)
-- Use Case: Đặt ngân sách giờ công cho công việc
-- Role: Quản lý dự án (VT-02)
-- ============================================================

-- Bổ sung cột budget_hours vào bảng tasks để lưu ngân sách giờ công
ALTER TABLE tasks 
    ADD COLUMN budget_hours DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

-- Ràng buộc giá trị ngân sách giờ công không được âm
ALTER TABLE tasks 
    ADD CONSTRAINT chk_tasks_budget_hours 
    CHECK (budget_hours >= 0);
