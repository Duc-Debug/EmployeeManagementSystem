-- ============================================================
-- FLYWAY MIGRATION V15: ADD EMPLOYEE PROFILE & CONTRACT FIELDS
-- ============================================================

ALTER TABLE employees ADD COLUMN professional_role VARCHAR(100) NULL;
ALTER TABLE employees ADD COLUMN start_date DATE NULL;
ALTER TABLE employees ADD COLUMN contract_end_date DATE NULL;

-- Backfill legacy rows before making standard working hours mandatory.
UPDATE employees
SET standard_hours_per_week = 40
WHERE standard_hours_per_week IS NULL;

-- Bổ sung constraint kiểm tra số giờ làm việc chuẩn phải > 0
ALTER TABLE employees
    ADD CONSTRAINT chk_employees_standard_hours
    CHECK (
        standard_hours_per_week IS NOT NULL
        AND standard_hours_per_week > 0
        AND standard_hours_per_week <= 168
    );
