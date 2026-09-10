-- ============================================================
-- FLYWAY MIGRATION V43: GRANT RESOURCE SEARCH & DEDUPLICATE SKILLS
-- ============================================================

-- 1. Grant RESOURCE_SEARCH permission to VT-02 (Project Manager) and VT-06 (Admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code IN ('VT-02', 'VT-06')
  AND p.code = 'RESOURCE_SEARCH'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 2. Deduplicate employee_skills mapping by matching stable skill codes ('SK-014' -> 'JAVA', 'SK-027' -> 'REACT', 'SK-055' -> 'DOCKER')
-- First, resolve unique constraint (employee_id, skill_id) conflicts:
-- Delete duplicate mapping if employee already has both duplicate skill and target canonical skill.
DELETE FROM employee_skills
WHERE id IN (
    SELECT es_dup.id FROM (
        SELECT es.id
        FROM employee_skills es
        JOIN skills s_dup ON s_dup.id = es.skill_id
        JOIN skills s_target ON (
            (s_dup.code = 'SK-014' AND s_target.code = 'JAVA') OR
            (s_dup.code = 'SK-027' AND s_target.code = 'REACT') OR
            (s_dup.code = 'SK-055' AND s_target.code = 'DOCKER')
        )
        WHERE EXISTS (
            SELECT 1 FROM employee_skills es2 
            WHERE es2.employee_id = es.employee_id AND es2.skill_id = s_target.id
        )
    ) es_dup
);

-- Safely reassign remaining duplicate mappings to canonical skill IDs
UPDATE employee_skills
SET skill_id = CASE 
    WHEN skill_id = (SELECT id FROM skills WHERE code = 'SK-014') THEN (SELECT id FROM skills WHERE code = 'JAVA')
    WHEN skill_id = (SELECT id FROM skills WHERE code = 'SK-027') THEN (SELECT id FROM skills WHERE code = 'REACT')
    WHEN skill_id = (SELECT id FROM skills WHERE code = 'SK-055') THEN (SELECT id FROM skills WHERE code = 'DOCKER')
    ELSE skill_id
END
WHERE skill_id IN (
    SELECT id FROM skills WHERE code IN ('SK-014', 'SK-027', 'SK-055')
);

-- 3. Remove duplicate skill catalog entries by business code
DELETE FROM skills WHERE code IN ('SK-014', 'SK-027', 'SK-055');
