-- ============================================================
-- FLYWAY MIGRATION V88: CREATE PROLONGED IDLENESS ACKNOWLEDGEMENTS SCHEMA
-- Epic: NCL-07 (Quản lý & Theo dõi Phân bổ)
-- Story: NCL-07-CN-006 (Cảnh báo nhân sự nhàn rỗi kéo dài - QTN-23)
-- ============================================================

CREATE TABLE IF NOT EXISTS prolonged_idleness_acknowledgements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    from_year INT NOT NULL,
    from_week INT NOT NULL,
    duration_weeks INT NOT NULL,
    action_taken VARCHAR(500) NOT NULL,
    notes TEXT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACKNOWLEDGED',
    acknowledged_by BIGINT NOT NULL,
    acknowledged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pia_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_pia_user FOREIGN KEY (acknowledged_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_pia_emp_period UNIQUE (employee_id, from_year, from_week, duration_weeks),
    INDEX idx_pia_lookup (employee_id, from_year, from_week),
    INDEX idx_pia_status (status)
);
