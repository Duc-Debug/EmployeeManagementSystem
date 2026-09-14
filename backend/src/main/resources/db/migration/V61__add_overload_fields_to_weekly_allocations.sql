-- ============================================================
-- FLYWAY MIGRATION V61: ADD OVERLOAD TRACKING FIELDS TO WEEKLY ALLOCATIONS
-- Epic: NCL-06 (Phan bo nguon luc theo tuan)
-- Story: NCL-06-CN-003 (Phat hien qua tai khi phan bo - QTN-11)
-- ============================================================

ALTER TABLE weekly_project_allocations ADD COLUMN is_overloaded BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE weekly_project_allocations ADD COLUMN overload_reason VARCHAR(1000) NULL;
ALTER TABLE weekly_project_allocations ADD COLUMN overload_approved_by BIGINT NULL;
ALTER TABLE weekly_project_allocations ADD COLUMN overload_approved_at TIMESTAMP NULL;

ALTER TABLE weekly_project_allocations
ADD CONSTRAINT fk_wpa_overload_approver
    FOREIGN KEY (overload_approved_by) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE weekly_project_allocations
ADD CONSTRAINT chk_overload_tracking
    CHECK (
        is_overloaded = FALSE 
        OR (
            is_overloaded = TRUE 
            AND overload_reason IS NOT NULL AND CHAR_LENGTH(TRIM(overload_reason)) > 0
            AND overload_approved_by IS NOT NULL 
            AND overload_approved_at IS NOT NULL
        )
    );

-- Permissions for Overload Bypass (QTN-11)
INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_ALLOCATION_OVERLOAD_BYPASS', 'Phê duyệt phân bổ vượt tải', 'Cho phép Quản lý nguồn lực xác nhận phân bổ giờ vượt năng lực khả dụng theo QTN-11'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'RESOURCE_ALLOCATION_OVERLOAD_BYPASS'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'RESOURCE_ALLOCATION_OVERLOAD_BYPASS'
WHERE r.code = 'VT-03'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

