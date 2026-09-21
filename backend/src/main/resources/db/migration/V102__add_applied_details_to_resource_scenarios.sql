-- ============================================================
-- FLYWAY MIGRATION V97: ADD APPLIED DETAILS TO RESOURCE SCENARIOS
-- Epic: NCL-08 (Mô phỏng kịch bản nhận thêm dự án)
-- Story: NCL-08-CN-003 (Áp dụng kịch bản thành phân bổ thật - QTN-14)
-- ============================================================

ALTER TABLE resource_scenarios ADD COLUMN target_project_id BIGINT NULL;
ALTER TABLE resource_scenarios ADD COLUMN applied_at TIMESTAMP NULL;
ALTER TABLE resource_scenarios ADD COLUMN applied_by BIGINT NULL;

ALTER TABLE resource_scenarios
    ADD CONSTRAINT fk_scenario_target_project FOREIGN KEY (target_project_id) REFERENCES projects(id) ON DELETE SET NULL;

ALTER TABLE resource_scenarios
    ADD CONSTRAINT fk_scenario_applied_by FOREIGN KEY (applied_by) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_scenario_target_project ON resource_scenarios (target_project_id);
