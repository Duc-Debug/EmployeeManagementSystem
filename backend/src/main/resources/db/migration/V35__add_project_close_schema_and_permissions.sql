-- ============================================================
-- FLYWAY MIGRATION V35: ADD PROJECT CLOSE & REOPEN SCHEMA AND PERMISSIONS
-- Epic: NCL-03 (Dự án và cây công việc)
-- Story: NCL-03-CN-004 (Đóng dự án & Mở lại dự án có kiểm soát)
-- Rule: QTN-08 (Không ghi giờ và không phân bổ vào dự án đã đóng)
-- ============================================================

-- 1. Bổ sung các cột lưu vết vòng đời đóng/mở lại dự án vào bảng projects
ALTER TABLE projects
    ADD COLUMN closure_reason VARCHAR(500) NULL,
    ADD COLUMN closed_at TIMESTAMP NULL,
    ADD COLUMN closed_by BIGINT NULL,
    ADD COLUMN reopen_reason VARCHAR(500) NULL,
    ADD COLUMN reopened_at TIMESTAMP NULL,
    ADD COLUMN reopened_by BIGINT NULL;

-- 2. Thêm khóa ngoại liên kết người đóng/mở với bảng users
ALTER TABLE projects
    ADD CONSTRAINT fk_projects_closed_by 
        FOREIGN KEY (closed_by) REFERENCES users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_projects_reopened_by 
        FOREIGN KEY (reopened_by) REFERENCES users(id) ON DELETE RESTRICT;

-- 3. Tạo chỉ mục để tối ưu tra cứu
CREATE INDEX idx_projects_closed_by ON projects(closed_by);
CREATE INDEX idx_projects_reopened_by ON projects(reopened_by);

-- 4. Bổ sung mã quyền PROJECT_CLOSE và PROJECT_REOPEN vào bảng permissions
INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_CLOSE', 'Đóng dự án', 'Cho phép kết thúc và đóng dự án, chốt giờ công và chi phí'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_CLOSE'
);

INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_REOPEN', 'Mở lại dự án', 'Cho phép mở lại dự án đã đóng (chỉ dành cho cấp quản trị)'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_REOPEN'
);

-- 5. Phân quyền PROJECT_CLOSE:
-- Cho phép VT-02 (PM phụ trách), VT-01 (Ban giám đốc đóng hộ), VT-06 (Admin đóng hộ)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'PROJECT_CLOSE'
WHERE r.code IN ('VT-02', 'VT-01', 'VT-06')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- 6. Phân quyền PROJECT_REOPEN:
-- CHỈ cho phép VT-01 (Ban giám đốc) và VT-06 (Admin), TUYỆT ĐỐI KHÔNG GÁN CHO VT-02 (PM)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'PROJECT_REOPEN'
WHERE r.code IN ('VT-01', 'VT-06')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);