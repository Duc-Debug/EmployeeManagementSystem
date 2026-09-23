-- NCL-10-CN-004 is restricted to VT-01 and VT-03.
-- V89 accidentally granted the report permission to VT-06 as well.
DELETE FROM role_permissions
WHERE role_id IN (SELECT id FROM roles WHERE code = 'VT-06')
  AND permission_id IN (
      SELECT id FROM permissions WHERE code = 'CAPACITY_FORECAST_REPORT_READ'
  );
