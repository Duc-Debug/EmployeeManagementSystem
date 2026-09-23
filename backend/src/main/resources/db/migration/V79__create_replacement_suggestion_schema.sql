-- ============================================================
-- FLYWAY MIGRATION V79: CREATE REPLACEMENT SUGGESTION SCHEMA
-- Epic: NCL-07 (Quản lý & Theo dõi Phân bổ)
-- Story: NCL-07-CN-002 (Đề xuất nhân sự thay thế)
-- ============================================================

-- 1. Bổ sung Permission mới cho Đề xuất nhân sự thay thế
INSERT INTO permissions (code, name, description)
SELECT 'RESOURCE_REPLACEMENT_SUGGEST', 'Gợi ý & Đề xuất nhân sự thay thế', 'Cho phép xem gợi ý và xác nhận đề xuất nhân sự thay thế khi xảy ra xung đột lịch'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'RESOURCE_REPLACEMENT_SUGGEST'
);

-- 2. Gán quyền cho VT-03 (Quản lý nguồn lực - RM) và VT-06 (Admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('VT-03', 'VT-06')
  AND p.code = 'RESOURCE_REPLACEMENT_SUGGEST'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. Tạo bảng schedule_conflict_replacements để lưu lịch sử đề xuất thay thế
CREATE TABLE IF NOT EXISTS schedule_conflict_replacements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conflict_id BIGINT NOT NULL,
    original_employee_id BIGINT NOT NULL,
    replacement_employee_id BIGINT NOT NULL,
    skill_id BIGINT NULL,
    proficiency_level INT NULL,
    free_hours DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'PROPOSED',
    notes TEXT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_repl_conflict FOREIGN KEY (conflict_id) REFERENCES schedule_conflict_warnings (id) ON DELETE CASCADE,
    CONSTRAINT fk_repl_orig_emp FOREIGN KEY (original_employee_id) REFERENCES employees (id),
    CONSTRAINT fk_repl_new_emp FOREIGN KEY (replacement_employee_id) REFERENCES employees (id),
    INDEX idx_repl_conflict (conflict_id),
    INDEX idx_repl_orig_emp (original_employee_id),
    INDEX idx_repl_new_emp (replacement_employee_id)
);
