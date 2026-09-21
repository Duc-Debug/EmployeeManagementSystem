-- ============================================================
-- FLYWAY MIGRATION V114: ADD SCHEDULE CONFIRMATION FEEDBACK & QTN-24
-- Feature: NCL-13-CN-002 (Xac nhan phan bo duoc giao - Quy tac QTN-24)
-- ============================================================

ALTER TABLE employee_schedule_confirmation 
    MODIFY COLUMN confirmed_at DATETIME(6) NULL;

ALTER TABLE employee_schedule_confirmation 
    ADD COLUMN feedback_note TEXT NULL;

ALTER TABLE employee_schedule_confirmation 
    ADD COLUMN feedback_at DATETIME(6) NULL;

ALTER TABLE employee_schedule_confirmation 
    ADD COLUMN confirmation_status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED';

-- Enforce MY_ALLOCATION_CONFIRM & MY_ALLOCATION_READ permissions
INSERT INTO permissions (code, name, description)
SELECT 'MY_ALLOCATION_READ', 'Xem phan bo ca nhan', 'Quyen xem lich va phan bo du an ca nhan'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'MY_ALLOCATION_READ');

INSERT INTO permissions (code, name, description)
SELECT 'MY_ALLOCATION_CONFIRM', 'Xac nhan phan bo ca nhan', 'Quyen xac nhan hoac phan hoi lich phan bo ca nhan'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'MY_ALLOCATION_CONFIRM');

-- Map permissions to roles VT-01 through VT-07
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04', 'VT-05', 'VT-06', 'VT-07')
  AND p.code IN ('MY_ALLOCATION_READ', 'MY_ALLOCATION_CONFIRM')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
