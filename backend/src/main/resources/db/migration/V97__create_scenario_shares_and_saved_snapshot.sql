-- ============================================================
-- FLYWAY MIGRATION V97: CREATE SCENARIO SHARES AND SAVED SNAPSHOT
-- Epic: NCL-08 (Mô phỏng kịch bản nhận thêm dự án)
-- Story: NCL-08-CN-006 (Lưu và chia sẻ kịch bản mô phỏng - QTN-14)
-- ============================================================

-- 1. Cập nhật bảng resource_scenarios: Thêm trạng thái 'saved' vào CHECK constraint
ALTER TABLE resource_scenarios DROP CONSTRAINT chk_scenario_status;
ALTER TABLE resource_scenarios 
    ADD CONSTRAINT chk_scenario_status 
    CHECK (status IN ('draft', 'saved', 'applied', 'discarded'));

-- 2. Thêm cột snapshot_data lưu toàn bộ kết quả mô phỏng khi Save và cột note ghi chú kịch bản
ALTER TABLE resource_scenarios 
    ADD COLUMN snapshot_data LONGTEXT NULL AFTER status,
    ADD COLUMN note TEXT NULL AFTER description;

-- 3. Tạo bảng scenario_shares lưu trữ quyền chia sẻ kịch bản ở chế độ VIEW_ONLY
CREATE TABLE IF NOT EXISTS scenario_shares (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    shared_with_user_id BIGINT NOT NULL,
    shared_by_user_id BIGINT NOT NULL,
    access_level VARCHAR(30) NOT NULL DEFAULT 'VIEW_ONLY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    is_active TINYINT(1) GENERATED ALWAYS AS (IF(revoked_at IS NULL, 1, NULL)) VIRTUAL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_share_scenario FOREIGN KEY (scenario_id) REFERENCES resource_scenarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_share_with_user FOREIGN KEY (shared_with_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_share_by_user FOREIGN KEY (shared_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_share_access_level CHECK (access_level IN ('VIEW_ONLY')),
    CONSTRAINT uk_scenario_active_share UNIQUE (scenario_id, shared_with_user_id, is_active)
);

CREATE INDEX idx_scenario_shares_scenario ON scenario_shares (scenario_id);
CREATE INDEX idx_scenario_shares_user ON scenario_shares (shared_with_user_id, revoked_at);

-- 4. Gán quyền RESOURCE_SCENARIO_READ cho VT-02 (Quản lý dự án) để xem kịch bản được chia sẻ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'RESOURCE_SCENARIO_READ'
  AND r.code = 'VT-02'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
