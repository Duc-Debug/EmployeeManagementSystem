-- ============================================================
-- FLYWAY MIGRATION V55: QUAN LY QUY PHEP NAM CUA NHAN SU (NCL-05-CN-005)
-- ============================================================

-- 1. Tạo bảng employee_leave_balances để lưu số ngày phép năm được hưởng của từng nhân sự
CREATE TABLE IF NOT EXISTS employee_leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    year_number INT NOT NULL,
    entitled_days DECIMAL(4,1) NOT NULL DEFAULT 12.0,     -- Số ngày phép được hưởng trong năm (tiền điều kiện Precondition)
    carried_over_days DECIMAL(4,1) NOT NULL DEFAULT 0.0, -- Số ngày phép năm trước được chuyển sang (nếu có)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_emp_leave_year UNIQUE (employee_id, year_number),
    CONSTRAINT fk_leave_balance_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT chk_entitled_days CHECK (entitled_days >= 0),
    CONSTRAINT chk_carried_over_days CHECK (carried_over_days >= 0)
);

CREATE INDEX idx_emp_leave_balances_lookup
    ON employee_leave_balances (employee_id, year_number);

-- 2. Khởi tạo dữ liệu mặc định năm 2026 cho tất cả nhân sự hiện có (12 ngày phép năm)
INSERT INTO employee_leave_balances (employee_id, year_number, entitled_days, carried_over_days)
SELECT e.id, 2026, 12.0, 0.0
FROM employees e
WHERE NOT EXISTS (
    SELECT 1 FROM employee_leave_balances elb 
    WHERE elb.employee_id = e.id AND elb.year_number = 2026
);

-- 3. Bổ sung mã quyền vào bảng permissions
INSERT INTO permissions (code, name, description)
SELECT 'LEAVE_BALANCE_READ', 'Xem quỹ ngày phép', 'Xem số ngày phép năm còn lại và lịch sử đã nghỉ'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'LEAVE_BALANCE_READ');

INSERT INTO permissions (code, name, description)
SELECT 'LEAVE_BALANCE_MANAGE', 'Quản lý quỹ ngày phép', 'Khai báo và điều chỉnh số ngày phép năm cho nhân viên'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'LEAVE_BALANCE_MANAGE');

-- 4. Gán quyền cho các vai trò theo quy định:
-- 4.1. VT-04 (Nhân viên chuyên môn): Quyền LEAVE_BALANCE_READ (kết hợp DataScope SELF ở application layer)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'LEAVE_BALANCE_READ'
WHERE r.code = 'VT-04'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 4.2. VT-03 (Quản lý nguồn lực): Quyền LEAVE_BALANCE_READ (kết hợp DataScope ORG_BRANCH)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'LEAVE_BALANCE_READ'
WHERE r.code = 'VT-03'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 4.3. VT-05 & VT-06 (Nhân sự & Ban giám đốc): Quyền LEAVE_BALANCE_READ & LEAVE_BALANCE_MANAGE (Scope ALL)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('LEAVE_BALANCE_READ', 'LEAVE_BALANCE_MANAGE')
WHERE r.code IN ('VT-05', 'VT-06')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 5. Tạo composite index trên bảng leave_requests phục vụ tính quỹ phép cực nhanh
CREATE INDEX idx_leave_requests_balance_lookup
    ON leave_requests (employee_id, leave_type, status, start_date);

