-- ============================================================
-- FLYWAY MIGRATION V82: CREATE CAPACITY THRESHOLD CONFIGS SCHEMA
-- Epic: NCL-07 (Cảnh báo xung đột lịch và đề xuất thay thế)
-- Story: NCL-07-CN-004 (Cấu hình ngưỡng cảnh báo quá tải và nhàn rỗi - QTN-23)
-- ============================================================

-- 1. Tạo bảng capacity_threshold_configs
CREATE TABLE IF NOT EXISTS capacity_threshold_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scope_type VARCHAR(30) NOT NULL DEFAULT 'COMPANY',
    scope_key VARCHAR(100) NOT NULL,
    org_unit_id BIGINT NULL,
    overload_threshold DECIMAL(5,1) NOT NULL,
    idle_threshold DECIMAL(5,1) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_ctc_scope_type CHECK (scope_type IN ('COMPANY', 'ORG_UNIT')),
    CONSTRAINT chk_ctc_threshold_range CHECK (
        idle_threshold >= 0 
        AND idle_threshold <= 100
        AND overload_threshold > 0
        AND overload_threshold <= 200 
        AND idle_threshold < overload_threshold
    ),
    CONSTRAINT uk_ctc_scope_key UNIQUE (scope_key),
    CONSTRAINT fk_ctc_org_unit FOREIGN KEY (org_unit_id) REFERENCES org_units(id) ON DELETE CASCADE,
    CONSTRAINT fk_ctc_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ctc_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE RESTRICT
);

-- Index tra cứu theo scope
CREATE INDEX idx_ctc_scope_lookup ON capacity_threshold_configs (scope_type, org_unit_id);

-- 2. Thêm Permissions
INSERT INTO permissions (code, name, description)
SELECT 'CAPACITY_THRESHOLD_MANAGE', 'Cấu hình ngưỡng cảnh báo năng lực', 'Cho phép Ban giám đốc (VT-01) cấu hình ngưỡng phần trăm quá tải và nhàn rỗi theo QTN-23'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'CAPACITY_THRESHOLD_MANAGE');

INSERT INTO permissions (code, name, description)
SELECT 'CAPACITY_THRESHOLD_READ', 'Xem cấu hình ngưỡng cảnh báo năng lực', 'Cho phép xem cấu hình ngưỡng phần trăm quá tải và nhàn rỗi theo QTN-23'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'CAPACITY_THRESHOLD_READ');

-- Gán quyền CAPACITY_THRESHOLD_MANAGE duy nhất cho VT-01 (Ban giám đốc)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'CAPACITY_THRESHOLD_MANAGE'
WHERE r.code = 'VT-01'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- Gán quyền CAPACITY_THRESHOLD_READ cho VT-01, VT-02, VT-03 (các vai trò được truy cập ma trận năng lực)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'CAPACITY_THRESHOLD_READ'
  AND r.code IN ('VT-01', 'VT-02', 'VT-03')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
