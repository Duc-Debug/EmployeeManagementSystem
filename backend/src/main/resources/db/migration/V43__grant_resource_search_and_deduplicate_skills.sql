-- ============================================================
-- FLYWAY MIGRATION V42: GRANT RESOURCE SEARCH & DEDUPLICATE SKILLS
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

-- 2. Deduplicate employee_skills mapping: merge duplicate skill IDs 7->1, 8->3, 9->6
DELETE FROM employee_skills 
WHERE skill_id = 7 
  AND employee_id IN (SELECT employee_id FROM (SELECT employee_id FROM employee_skills WHERE skill_id = 1) t);
UPDATE employee_skills SET skill_id = 1 WHERE skill_id = 7;

DELETE FROM employee_skills 
WHERE skill_id = 8 
  AND employee_id IN (SELECT employee_id FROM (SELECT employee_id FROM employee_skills WHERE skill_id = 3) t);
UPDATE employee_skills SET skill_id = 3 WHERE skill_id = 8;

DELETE FROM employee_skills 
WHERE skill_id = 9 
  AND employee_id IN (SELECT employee_id FROM (SELECT employee_id FROM employee_skills WHERE skill_id = 6) t);
UPDATE employee_skills SET skill_id = 6 WHERE skill_id = 9;

-- 3. Remove duplicate skill catalog entries
DELETE FROM skills WHERE id IN (7, 8, 9);
