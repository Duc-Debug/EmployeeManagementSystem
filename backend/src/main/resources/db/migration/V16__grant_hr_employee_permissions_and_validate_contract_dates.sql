-- Normalize legacy rows before enforcing the contract date invariant.
-- These columns are nullable, so an unknown/invalid end date is represented as NULL.
UPDATE employees
SET contract_end_date = NULL
WHERE contract_end_date IS NOT NULL
  AND start_date IS NOT NULL
  AND contract_end_date < start_date;

ALTER TABLE employees
    ADD CONSTRAINT chk_employees_contract_dates
    CHECK (contract_end_date IS NULL OR start_date IS NULL OR contract_end_date >= start_date);

-- VT-05 (Nhân sự) là vai trò nghiệp vụ quản lý hồ sơ nhân viên.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON p.code IN ('EMPLOYEE_READ', 'EMPLOYEE_UPDATE')
WHERE r.code = 'VT-05'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
