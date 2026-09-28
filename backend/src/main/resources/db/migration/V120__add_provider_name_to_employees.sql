-- ============================================================
-- FLYWAY MIGRATION V120: ADD PROVIDER NAME TO EMPLOYEES
-- User Story: NCL-14-CN-001 (Khai báo hồ sơ nhân sự thuê ngoài)
-- ============================================================

ALTER TABLE employees ADD COLUMN provider_name VARCHAR(255) NULL;

CREATE INDEX idx_employees_is_outsourced ON employees(is_outsourced);
