-- ============================================================
-- FLYWAY MIGRATION V56: GRANT DEPARTMENT LEAVE CALENDAR READ TO VT-05 (HR)
-- ============================================================

-- Grant DEPARTMENT_LEAVE_READ to VT-05 (Nhân sự)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'DEPARTMENT_LEAVE_READ'
  AND r.code = 'VT-05'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
