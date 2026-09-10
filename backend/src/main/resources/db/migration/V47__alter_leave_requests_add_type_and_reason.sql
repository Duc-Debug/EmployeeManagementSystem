-- ============================================================
-- FLYWAY MIGRATION V47: BO SUNG THONG TIN DON NGHI PHEP (NCL-05-CN-002)
-- ============================================================

-- 1. Bo sung cot neu chua ton tai
DROP PROCEDURE IF EXISTS upgrade_leave_requests_v47;
DELIMITER //
CREATE PROCEDURE upgrade_leave_requests_v47()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'leave_requests' AND column_name = 'leave_type'
    ) THEN
        ALTER TABLE leave_requests
            ADD COLUMN leave_type VARCHAR(50) NOT NULL DEFAULT 'ANNUAL' AFTER employee_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'leave_requests' AND column_name = 'reason'
    ) THEN
        ALTER TABLE leave_requests
            ADD COLUMN reason VARCHAR(500) NULL AFTER hours_deducted;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE table_schema = DATABASE() AND table_name = 'leave_requests' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE leave_requests
            ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;
    END IF;
END //
DELIMITER ;

CALL upgrade_leave_requests_v47();
DROP PROCEDURE IF EXISTS upgrade_leave_requests_v47;

-- 2. Dat rang buoc kiem tra hop le cho leave_type (bo qua neu da co)
-- 3. Them ma quyen LEAVE_REQUEST_CREATE
INSERT INTO permissions (code, name, description)
SELECT 'LEAVE_REQUEST_CREATE', 'Gửi đơn nghỉ phép', 'Cho phép nhân viên chuyên môn nộp đơn xin nghỉ phép cá nhân'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'LEAVE_REQUEST_CREATE');

-- 4. Gan quyen cho VT-04
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'LEAVE_REQUEST_CREATE'
WHERE r.code = 'VT-04'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );