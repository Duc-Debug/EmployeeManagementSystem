-- ============================================================
-- FLYWAY MIGRATION V39: SEED ADDITIONAL PROJECT TEMPLATES
-- Epic: NCL-03 (Dự án và cây công việc)
-- Story: NCL-03-CN-005 (Tạo dự án từ mẫu)
-- ============================================================

-- 1. Seed Mẫu dự án số 2: "Mẫu dự án bảo trì và vận hành hệ thống"
INSERT INTO project_templates (id, template_code, name, description, is_active, version)
VALUES (2, 'TPL-OPS-002', 'Mẫu dự án bảo trì và vận hành hệ thống', 'Mẫu chuẩn cho dịch vụ SLA, bảo trì định kỳ, nâng cấp bản vá và hỗ trợ kỹ thuật', TRUE, 0);

-- Hạng mục 1: Tiếp nhận và bàn giao vận hành (id: 10)
INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (10, 2, NULL, 'Tiếp nhận & Bàn giao vận hành', 'Kiểm tra tài liệu kỹ thuật, chuyển giao mã nguồn và cấu hình môi trường', 'CATEGORY', 0.00, 1);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (11, 2, 10, 'Rà soát tài liệu hệ thống & kiến trúc hạ tầng', 'Nắm bắt hiện trạng máy chủ, CSDL và các dịch vụ tích hợp', 'TASK', 12.00, 1);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (12, 2, 10, 'Thiết lập quy trình giám sát & trực hỗ trợ SLA', 'Cấu hình cảnh báo uptime, quy định phản hồi sự cố cấp độ 1-3', 'TASK', 8.00, 2);

-- Hạng mục 2: Giám sát, tối ưu & cập nhật bản vá định kỳ (id: 13)
INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (13, 2, NULL, 'Giám sát, tối ưu & Cập nhật bản vá', 'Các hoạt động duy trì tính sẵn sàng và an toàn bảo mật', 'CATEGORY', 0.00, 2);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (14, 2, 13, 'Kiểm tra log hệ thống & sao lưu định kỳ', 'Kiểm tra backup CSDL và dọn dẹp dung lượng lưu trữ', 'TASK', 16.00, 1);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (15, 2, 13, 'Áp dụng bản vá bảo mật và nâng cấp thư viện phụ thuộc', 'Cập nhật phiên bản các gói phần mềm phòng chống lỗ hổng bảo mật', 'TASK', 24.00, 2);


-- 2. Seed Mẫu dự án số 3: "Mẫu dự án triển khai ERP & Chuyển đổi số"
INSERT INTO project_templates (id, template_code, name, description, is_active, version)
VALUES (3, 'TPL-ERP-003', 'Mẫu dự án triển khai ERP & Chuyển đổi số', 'Mẫu toàn diện từ chuẩn hóa dữ liệu, tùy biến giải pháp, đào tạo đến Go-Live', TRUE, 0);

-- Hạng mục 1: Chuẩn hóa quy trình nghiệp vụ & Dữ liệu tổng thể (id: 16)
INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (16, 3, NULL, 'Chuẩn hóa quy trình & Dữ liệu chủ', 'Giai đoạn khảo sát quy trình các phòng ban và làm sạch dữ liệu cũ', 'CATEGORY', 0.00, 1);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (17, 3, 16, 'Khảo sát và lập ma trận quy trình As-Is / To-Be', 'Làm việc cùng các trưởng bộ phận xác định luồng dữ liệu chuẩn', 'TASK', 24.00, 1);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (18, 3, 16, 'Làm sạch và chuyển đổi dữ liệu kế thừa (Migration)', 'Trích xuất, chuyển đổi và nạp dữ liệu nhân sự, khách hàng, tồn kho cũ', 'TASK', 32.00, 2);

-- Hạng mục 2: Tùy biến và Tích hợp hệ thống (id: 19)
INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (19, 3, NULL, 'Tùy biến giải pháp & Tích hợp liên thông', 'Cài đặt phân hệ cốt lõi và kết nối các ứng dụng hiện hữu', 'CATEGORY', 0.00, 2);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (20, 3, 19, 'Cấu hình tham số hệ thống & phân quyền phân hệ', 'Thiết lập các cây tổ chức, quy tắc duyệt và danh mục dùng chung', 'TASK', 40.00, 1);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (21, 3, 19, 'Xây dựng API kết nối cổng thanh toán & hóa đơn điện tử', 'Tích hợp các cổng dịch vụ bên thứ ba', 'TASK', 36.00, 2);

-- Hạng mục 3: Đào tạo, UAT và Vận hành chính thức (Go-Live) (id: 22)
INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (22, 3, NULL, 'Đào tạo người dùng, UAT & Go-Live', 'Huấn luyện người dùng cuối và chuyển đổi vận hành chính thức', 'CATEGORY', 0.00, 3);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (23, 3, 22, 'Tổ chức khóa đào tạo người dùng chủ chốt (Key Users)', 'Soạn thảo giáo trình và hướng dẫn thực hành quy trình chuẩn', 'TASK', 20.00, 1);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (24, 3, 22, 'Kiểm thử chấp nhận người dùng (UAT) toàn diện', 'Người dùng chạy thử kịch bản nghiệp vụ thực tế', 'TASK', 28.00, 2);

INSERT INTO project_template_tasks (id, template_id, parent_id, name, description, task_type, estimated_hours, sort_order)
VALUES (25, 3, 22, 'Hỗ trợ chuyển đổi dữ liệu chốt kỳ và Go-Live', 'Khai trương vận hành chính thức và hỗ trợ on-site', 'TASK', 20.00, 3);
