-- ============================================================
-- FLYWAY MIGRATION V32: CREATE PROJECT TEMPLATES SCHEMA
-- Epic: NCL-03 (Dự án và cây công việc)
-- Story: NCL-03-CN-005 (Tạo dự án từ mẫu)
-- ============================================================

-- 1. Tạo bảng mẫu dự án (project_templates)
CREATE TABLE IF NOT EXISTS project_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_project_templates_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE SET NULL
);

-- 2. Tạo bảng công việc mẫu trong cây WBS (project_template_tasks)
CREATE TABLE IF NOT EXISTS project_template_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    task_type VARCHAR(20) NOT NULL DEFAULT 'TASK',
    estimated_hours DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_template_tasks_template
        FOREIGN KEY (template_id)
        REFERENCES project_templates(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_template_tasks_parent
        FOREIGN KEY (parent_id)
        REFERENCES project_template_tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_template_tasks_type
        CHECK (task_type IN ('CATEGORY', 'TASK')),

    CONSTRAINT chk_template_tasks_estimated_hours
        CHECK (estimated_hours >= 0),

    CONSTRAINT chk_template_tasks_sort_order
        CHECK (sort_order >= 0)
);

-- 3. Chỉ mục tối ưu truy vấn cây công việc mẫu
CREATE INDEX idx_template_tasks_template_id 
    ON project_template_tasks(template_id);

CREATE INDEX idx_template_tasks_parent_id 
    ON project_template_tasks(parent_id);

CREATE INDEX idx_template_tasks_template_parent_sort 
    ON project_template_tasks(template_id, parent_id, sort_order);

-- 4. Seed dữ liệu mẫu: "Mẫu dự án triển khai phần mềm" (AC-01 / TC-01)
INSERT INTO project_templates (id, template_code, name, description, is_active, version)
VALUES (1, 'TPL-DEV-001', 'Mẫu dự án triển khai phần mềm', 'Mẫu chuẩn gồm 3 giai đoạn: Khảo sát, Phát triển và Nghiệm thu', TRUE, 0);

-- 4.1. Hạng mục 1: Khởi động & Khảo sát yêu cầu (id: 1)
INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (1, 1, NULL, 'Khởi động & Khảo sát yêu cầu', 'Giai đoạn thu thập yêu cầu và làm rõ nghiệp vụ', 'CATEGORY', 16.00, 1);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (2, 1, 1, 'Khảo sát hiện trạng & Quy trình', 'Làm việc với khách hàng ghi nhận quy trình', 'TASK', 8.00, 1);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (3, 1, 1, 'Viết tài liệu đặc tả yêu cầu', 'Hoàn thiện tài liệu SRS', 'TASK', 8.00, 2);

-- 4.2. Hạng mục 2: Thiết kế & Phát triển (id: 4)
INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (4, 1, NULL, 'Thiết kế & Phát triển hệ thống', 'Giai đoạn thiết kế kiến trúc và viết code', 'CATEGORY', 80.00, 2);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (5, 1, 4, 'Thiết kế kiến trúc & CSDL', 'Thiết kế ERD và mô hình dịch vụ', 'TASK', 24.00, 1);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (6, 1, 4, 'Lập trình tính năng Backend & Frontend', 'Triển khai mã nguồn chức năng', 'TASK', 56.00, 2);

-- 4.3. Hạng mục 3: Kiểm thử & Nghiệm thu (id: 7)
INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (7, 1, NULL, 'Kiểm thử & Nghiệm thu bàn giao', 'Giai đoạn test hệ thống và triển khai UAT', 'CATEGORY', 24.00, 3);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (8, 1, 7, 'Kiểm thử tích hợp hệ thống (SIT)', 'Thực hiện test case và sửa lỗi tồn đọng', 'TASK', 16.00, 1);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (9, 1, 7, 'Hỗ trợ nghiệm thu người dùng (UAT)', 'Hỗ trợ khách hàng test và ký biên bản nghiệm thu', 'TASK', 8.00, 2);