-- Ensure the legacy root department exists as its own org unit under the
-- company root. It must not be treated as COMPANY_ROOT just because ids match.
INSERT INTO org_units (
    unit_code,
    unit_name,
    unit_type,
    parent_id,
    tree_path,
    level,
    status,
    description,
    created_at
)
SELECT
    'PB-01',
    'Ban giám đốc',
    'DEPARTMENT',
    root.id,
    CONCAT(root.tree_path, 'pending/'),
    root.level + 1,
    'ACTIVE',
    'Legacy department PB-01 migrated to org_units',
    CURRENT_TIMESTAMP
FROM org_units root
WHERE root.unit_code = 'COMPANY_ROOT'
  AND NOT EXISTS (
      SELECT 1
      FROM org_units existing
      WHERE existing.unit_code = 'PB-01'
  );

UPDATE org_units
SET unit_name = 'Ban giám đốc',
    unit_type = 'DEPARTMENT',
    parent_id = (
        SELECT root_id
        FROM (
            SELECT id AS root_id
            FROM org_units
            WHERE unit_code = 'COMPANY_ROOT'
        ) root
    ),
    level = (
        SELECT root_level + 1
        FROM (
            SELECT level AS root_level
            FROM org_units
            WHERE unit_code = 'COMPANY_ROOT'
        ) root
    ),
    tree_path = CONCAT(
        (
            SELECT root_tree_path
            FROM (
                SELECT tree_path AS root_tree_path
                FROM org_units
                WHERE unit_code = 'COMPANY_ROOT'
            ) root
        ),
        id,
        '/'
    ),
    status = 'ACTIVE'
WHERE unit_code = 'PB-01';

-- Backfill legacy employees by business key only.
-- departments.id and org_units.id are independent technical identifiers.
UPDATE employees
SET org_unit_id = (
    SELECT mapped_org_unit_id
    FROM (
        SELECT d.id AS mapped_department_id,
               ou.id AS mapped_org_unit_id
        FROM departments d
        JOIN org_units ou
            ON ou.unit_code = d.code
    ) mapped
    WHERE mapped.mapped_department_id = employees.department_id
)
WHERE org_unit_id IS NULL
  AND department_id IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM (
          SELECT d.id AS mapped_department_id
          FROM departments d
          JOIN org_units ou
              ON ou.unit_code = d.code
      ) mapped
      WHERE mapped.mapped_department_id = employees.department_id
  );

-- Fail fast if any legacy ownership still cannot be mapped explicitly.
ALTER TABLE employees
ADD CONSTRAINT chk_employees_org_unit_backfilled
CHECK (
    department_id IS NULL
    OR org_unit_id IS NOT NULL
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
