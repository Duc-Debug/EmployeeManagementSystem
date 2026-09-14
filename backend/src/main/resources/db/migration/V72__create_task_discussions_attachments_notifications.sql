-- ============================================================
-- FLYWAY MIGRATION V53: CREATE TASK DISCUSSIONS, ATTACHMENTS & NOTIFICATIONS
-- Epic: NCL-04 (Quản lý Tiến độ & Công việc Dự án)
-- Use Case: Trao đổi trực tiếp trên công việc (Task Discussions, Mentions & Attachments)
-- ============================================================

-- 1. Bảng lưu trữ ghi chú / trao đổi trên công việc
CREATE TABLE IF NOT EXISTS task_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_task_comments_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_comments_author
        FOREIGN KEY (author_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
);

-- 2. Bảng lưu trữ tệp đính kèm / tệp mô phỏng theo task hoặc theo trao đổi
CREATE TABLE IF NOT EXISTS task_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NULL,
    task_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(100) NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_attachments_comment
        FOREIGN KEY (comment_id)
        REFERENCES task_comments(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_attachments_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_attachments_user
        FOREIGN KEY (uploaded_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
);

-- 3. Bảng lưu quan hệ nhắc tên đồng nghiệp (@mention) trong trao đổi
CREATE TABLE IF NOT EXISTS task_comment_mentions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    mentioned_user_id BIGINT NOT NULL,

    CONSTRAINT fk_mentions_comment
        FOREIGN KEY (comment_id)
        REFERENCES task_comments(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_mentions_user
        FOREIGN KEY (mentioned_user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_comment_mention
        UNIQUE (comment_id, mentioned_user_id)
);

-- 4. Bảng thông báo (In-App Notifications)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    sender_id BIGINT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'TASK_MENTION',
    target_type VARCHAR(50) NOT NULL DEFAULT 'TASK',
    target_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notifications_sender
        FOREIGN KEY (sender_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);

-- 5. Chỉ mục tối ưu truy vấn timeline và thông báo
CREATE INDEX idx_task_comments_task_id ON task_comments(task_id);
CREATE INDEX idx_task_attachments_task_id ON task_attachments(task_id);
CREATE INDEX idx_task_attachments_comment_id ON task_attachments(comment_id);
CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_id, is_read, created_at);

-- 6. Phân quyền truy cập và trao đổi trên công việc
INSERT INTO permissions (code, name, description)
SELECT 'TASK_DISCUSSION_READ', 'Xem trao đổi công việc', 'Cho phép xem dòng thời gian trao đổi trên công việc'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'TASK_DISCUSSION_READ'
);

INSERT INTO permissions (code, name, description)
SELECT 'TASK_DISCUSSION_CREATE', 'Trao đổi trên công việc', 'Cho phép viết ghi chú, đính kèm tệp và nhắc tên đồng nghiệp trên công việc'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'TASK_DISCUSSION_CREATE'
);

-- Gán quyền cho các vai trò: Ban giám đốc (VT-01), Quản lý dự án (VT-02), Quản lý nguồn lực (VT-03), Nhân viên chuyên môn (VT-04)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('VT-01', 'VT-02', 'VT-03', 'VT-04')
  AND p.code IN ('TASK_DISCUSSION_READ', 'TASK_DISCUSSION_CREATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

