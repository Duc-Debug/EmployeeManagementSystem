-- 1. Bổ sung các cột mới cho bảng projects
ALTER TABLE projects ADD COLUMN start_date DATE NULL;
ALTER TABLE projects ADD COLUMN end_date DATE NULL;
ALTER TABLE projects ADD COLUMN estimated_hours DECIMAL(10, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE projects ADD COLUMN description TEXT NULL;

-- 2. Thêm ràng buộc kiểm tra ngày kết thúc >= ngày bắt đầu
ALTER TABLE projects
    ADD CONSTRAINT chk_projects_dates
        CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date);

-- 3. Bổ sung Permission PROJECT_CREATE
INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_CREATE', 'Tạo dự án', 'Cho phép tạo mới dự án trong hệ thống'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_CREATE'
);

-- 4. Gán quyền PROJECT_CREATE cho vai trò VT-02 (Quản lý dự án)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'PROJECT_CREATE'
WHERE r.code IN ('VT-02')
AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);