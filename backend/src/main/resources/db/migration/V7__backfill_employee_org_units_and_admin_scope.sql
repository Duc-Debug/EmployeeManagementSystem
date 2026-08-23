-- Backfill ownership data after introducing org_units-based employee ownership.
-- Existing rows that already point to org_units are kept unchanged.
UPDATE employees
SET org_unit_id = department_id
WHERE org_unit_id IS NULL
  AND department_id IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM org_units
      WHERE org_units.id = employees.department_id
  );

-- If a legacy department cannot be mapped to an org_unit by id, keep the
-- employee visible under the company root instead of disappearing from
-- org_unit_id-based INNER JOIN scope queries.
UPDATE employees
SET org_unit_id = (
    SELECT id
    FROM org_units
    WHERE unit_code = 'COMPANY_ROOT'
)
WHERE org_unit_id IS NULL
  AND department_id IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM org_units
      WHERE unit_code = 'COMPANY_ROOT'
  );

-- Existing system administrators must remain operational after V6 adds
-- data_scope with a SELF default. RBAC still comes from role_permissions;
-- this only scopes already-existing VT-06 accounts to company data.
UPDATE users
SET data_scope = 'COMPANY',
    scope_org_unit_id = NULL
WHERE role_id IN (
    SELECT id
    FROM roles
    WHERE code = 'VT-06'
);
