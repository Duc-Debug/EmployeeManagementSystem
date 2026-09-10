-- ============================================================
-- FLYWAY MIGRATION V40: CORRECT PROJECT_CREATE PERMISSIONS PER RBAC SPEC
-- Tham chiếu: docs/ROLE_BASED_ACCESS_CONTROL_GUIDE.md — Ma trận Phân Quyền (dòng 45)
--
-- Ma trận /projects:
--   VT-01 (Ban giám đốc)    → 👁️ Xem  → KHÔNG có PROJECT_CREATE
--   VT-02 (PM)              → ✅ Toàn quyền dự án của mình → CÓ PROJECT_CREATE
--   VT-03 (Quản lý NL)      → 👁️ Xem  → KHÔNG có PROJECT_CREATE
--   VT-04 (Nhân viên)       → 👁️ Dự án tham gia → KHÔNG có PROJECT_CREATE
--   VT-05 (Nhân sự)         → ❌       → KHÔNG có PROJECT_CREATE
--   VT-06 (Quản trị viên)   → ❌       → KHÔNG có PROJECT_CREATE
--
-- V36 đã grant sai PROJECT_CREATE cho VT-01, VT-03, VT-06.
-- Migration này sửa lại cho đúng chuẩn tài liệu.
-- ============================================================

-- 1. Đảm bảo permission PROJECT_CREATE tồn tại (idempotent)
INSERT INTO permissions (code, name, description)
SELECT 'PROJECT_CREATE', 'Tạo dự án', 'Cho phép tạo mới dự án trong hệ thống'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PROJECT_CREATE'
);

-- 2. Đảm bảo VT-02 (PM) có PROJECT_CREATE — vai trò duy nhất được phép tạo dự án (idempotent)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'PROJECT_CREATE'
  AND r.code = 'VT-02'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- 3. Thu hồi PROJECT_CREATE khỏi các role không được phép theo tài liệu RBAC:
--    VT-01 (Ban giám đốc): 👁️ Xem — chỉ đọc
--    VT-03 (Quản lý NL):   👁️ Xem — chỉ đọc
--    VT-04 (Nhân viên):    👁️ Dự án tham gia — chỉ đọc
--    VT-05 (Nhân sự):      ❌ — không truy cập
--    VT-06 (Admin):        ❌ — không truy cập (Admin quản lý TK, không quản lý dự án)
DELETE FROM role_permissions
WHERE role_id IN (
    SELECT id FROM roles WHERE code IN ('VT-01', 'VT-03', 'VT-04', 'VT-05', 'VT-06')
)
  AND permission_id IN (
      SELECT id FROM permissions WHERE code = 'PROJECT_CREATE'
  );
