-- ============================================================
-- FLYWAY MIGRATION V36: NORMALIZE ALL USER DATA_SCOPE & SCOPE_ORG_UNIT_ID
-- ============================================================

-- 1. Chuan hoa DataScope = 'COMPANY' cho VT-01 (Ban Giam doc), VT-05 (HR Admin), VT-06 (System Admin)
UPDATE users
SET data_scope = 'COMPANY',
    scope_org_unit_id = NULL
WHERE role_id IN (
    SELECT id FROM roles WHERE code IN ('VT-01', 'VT-05', 'VT-06')
)
  AND (data_scope <> 'COMPANY' OR scope_org_unit_id IS NOT NULL);

-- 2. Chuan hoa DataScope = 'SELF' cho VT-02 (Project Manager), VT-04 (Employee)
UPDATE users
SET data_scope = 'SELF',
    scope_org_unit_id = NULL
WHERE role_id IN (
    SELECT id FROM roles WHERE code IN ('VT-02', 'VT-04')
)
  AND (data_scope <> 'SELF' OR scope_org_unit_id IS NOT NULL);

-- 3. Chuan hoa DataScope = 'ORGANIZATION_BRANCH' cho VT-03 (Department Head)
UPDATE users
SET data_scope = 'ORGANIZATION_BRANCH'
WHERE role_id IN (
    SELECT id FROM roles WHERE code = 'VT-03'
)
  AND data_scope <> 'ORGANIZATION_BRANCH';

-- 4. Dong bo scope_org_unit_id cho VT-03 neu dang NULL
UPDATE users u
SET scope_org_unit_id = COALESCE(
    (SELECT e.org_unit_id FROM employees e WHERE e.user_id = u.id LIMIT 1),
    (SELECT id FROM org_units WHERE unit_code = 'COMPANY_ROOT' LIMIT 1),
    (SELECT id FROM org_units ORDER BY id ASC LIMIT 1)
)
WHERE role_id IN (
    SELECT id FROM roles WHERE code = 'VT-03'
)
  AND scope_org_unit_id IS NULL;
