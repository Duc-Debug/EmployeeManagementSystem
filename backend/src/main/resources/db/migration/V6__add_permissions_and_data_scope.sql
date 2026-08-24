CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),

    CONSTRAINT uk_permissions_code UNIQUE (code)
);
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id)
        REFERENCES permissions(id)
        ON DELETE CASCADE
);
INSERT INTO permissions (code, name, description)
VALUES
('USER_READ',
 'Xem tài khoản',
 'Cho phép xem danh sách và thông tin tài khoản'),

('USER_CREATE',
 'Tạo tài khoản',
 'Cho phép tạo tài khoản mới'),

('USER_UPDATE_ROLE',
 'Phân quyền tài khoản',
 'Cho phép thay đổi vai trò và phạm vi dữ liệu của tài khoản'),

('USER_TOGGLE_STATUS',
 'Khóa hoặc mở tài khoản',
 'Cho phép thay đổi trạng thái hoạt động của tài khoản'),

('ORG_UNIT_READ',
 'Xem cơ cấu tổ chức',
 'Cho phép xem cây tổ chức'),

('EMPLOYEE_READ',
 'Xem nhân viên',
 'Cho phép xem thông tin nhân viên'),

('EMPLOYEE_UPDATE',
 'Cập nhật nhân viên',
 'Cho phép cập nhật thông tin nhân viên');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'VT-06'
AND p.code IN (
    'USER_READ',
    'USER_CREATE',
    'USER_UPDATE_ROLE',
    'USER_TOGGLE_STATUS',
    'ORG_UNIT_READ',
    'EMPLOYEE_READ',
    'EMPLOYEE_UPDATE'
);

ALTER TABLE users
ADD COLUMN data_scope VARCHAR(30) NOT NULL DEFAULT 'SELF';

ALTER TABLE users
ADD COLUMN scope_org_unit_id BIGINT NULL;

ALTER TABLE users
ADD CONSTRAINT fk_users_scope_org_unit
FOREIGN KEY (scope_org_unit_id)
REFERENCES org_units(id)
ON DELETE RESTRICT;


CREATE INDEX idx_users_scope_org_unit_id
ON users(scope_org_unit_id);

ALTER TABLE users
ADD CONSTRAINT chk_users_data_scope
CHECK (
    data_scope IN (
        'COMPANY',
        'ORGANIZATION_BRANCH',
        'SELF'
    )
);


ALTER TABLE users
ADD CONSTRAINT chk_users_scope_consistency
CHECK (
    (
        data_scope = 'ORGANIZATION_BRANCH'
        AND scope_org_unit_id IS NOT NULL
    )
    OR
    (
        data_scope IN ('COMPANY', 'SELF')
        AND scope_org_unit_id IS NULL
    )
);