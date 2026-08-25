-- 1. Tạo bảng org_units

CREATE TABLE IF NOT EXISTS org_units (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    unit_code VARCHAR(50) NOT NULL UNIQUE,
    unit_name VARCHAR(255) NOT NULL,
    unit_type VARCHAR(50) NOT NULL,

    parent_id BIGINT NULL,

    tree_path VARCHAR(500) NOT NULL,
    level INT NOT NULL DEFAULT 1,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_org_units_parent FOREIGN KEY (parent_id) REFERENCES org_units(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Index tối ưu truy vấn cây

CREATE INDEX idx_org_units_parent_id
    ON org_units(parent_id);

CREATE INDEX idx_org_units_tree_path
    ON org_units(tree_path);

CREATE INDEX idx_org_units_status
    ON org_units(status);

-- 3. Khởi tạo nút gốc mặc định

INSERT INTO org_units (
    unit_code,
    unit_name,
    unit_type,
    parent_id,
    tree_path,
    level,
    status,
    description,
    created_at
)
VALUES (
    'COMPANY_ROOT',
    'Công Ty Cổ Phần Software',
    'COMPANY',
    NULL,
    '/pending/',
    1,
    'ACTIVE',
    'Nút gốc của Cây tổ chức',
    CURRENT_TIMESTAMP
);

UPDATE org_units
SET tree_path = CONCAT('/', id, '/')
WHERE unit_code = 'COMPANY_ROOT';
