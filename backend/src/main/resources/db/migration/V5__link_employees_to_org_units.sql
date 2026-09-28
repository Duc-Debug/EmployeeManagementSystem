ALTER TABLE employees
ADD COLUMN org_unit_id BIGINT NULL AFTER department_id;

ALTER TABLE employees
ADD CONSTRAINT fk_employees_org_unit
FOREIGN KEY (org_unit_id)
REFERENCES org_units(id)
ON DELETE RESTRICT;

CREATE INDEX idx_employees_org_unit_id
ON employees(org_unit_id);