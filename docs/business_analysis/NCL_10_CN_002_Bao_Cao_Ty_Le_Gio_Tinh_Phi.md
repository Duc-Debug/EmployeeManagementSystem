# Kế hoạch triển khai NCL-10-CN-002: Báo cáo tỷ lệ giờ tính phí (Billable Utilization Rate Report)

## 1. Thông tin chung

- **Tên chức năng**: Báo cáo tỷ lệ giờ tính phí
- **Mã User Story**: NCL-10-CN-002
- **Epic**: NCL-10 - Báo cáo năng lực và bảng điều khiển
- **Vai trò chính**: Ban giám đốc (VT-01)
- **Vai trò được xem**: Ban giám đốc (VT-01), Quản lý nguồn lực (VT-03), Quản trị viên (VT-06)
- **Độ ưu tiên**: Sống còn (High Priority)
- **Story Point**: 5
- **Chu kỳ**: Chu kỳ số sáu

## 2. Mục tiêu nghiệp vụ

Là Ban Giám Đốc của công ty dịch vụ công nghệ / giải pháp, chỉ số tỷ lệ giờ tính phí (Billable Utilization Rate) là **chỉ số sống còn**, cho biết bao nhiêu phần trăm thời gian khả dụng của nhân sự thực sự tạo ra doanh thu từ khách hàng.

Báo cáo cho phép:
1. Xem tỷ lệ giờ tính phí theo **Toàn công ty** (Company Overview).
2. Phân rã và so sánh theo **Từng bộ phận / phòng ban** (Department Breakdown).
3. Xem chi tiết tỷ lệ và số giờ của **Từng nhân viên** (Employee Breakdown).
4. Phân loại rõ ràng: Giờ chuẩn, Giờ nghỉ lễ, Giờ nghỉ phép đã duyệt, Giờ khả dụng ròng, Giờ tính phí (Billable), Giờ không tính phí (Non-billable).

---

## 3. Công thức tính toán & Quy tắc nghiệp vụ

### BR-01. Công thức tỷ lệ giờ tính phí của nhân sự
- Tỷ lệ giờ tính phí (%) = (Approved Billable Hours / Net Available Hours) * 100%
- Approved Billable Hours: Tổng số giờ từ timesheet_entries có status = 'APPROVED' và is_billable = TRUE trong khoảng thời gian phân tích.
- Approved Non-Billable Hours: Tổng số giờ từ timesheet_entries có status = 'APPROVED' và is_billable = FALSE (hoạt động nội bộ, đào tạo...).
- Net Available Hours: Tổng số giờ khả dụng thực tế của nhân sự trong kỳ sau khi trừ ngày lễ và nghỉ phép đã duyệt.
  Net Available Hours = Sum(max(0, Standard Hours - Holiday Hours - Approved Leave Hours))

### BR-02. Xử lý ngoại lệ nghỉ phép (NCL-10-CN-002-TC-02)
- Khi một nhân viên nghỉ phép (đơn nghỉ phép đã APPROVED), số giờ nghỉ phép được trừ khỏi mẫu số (giờ khả dụng) thay vì tính là giờ lãng phí hay không hiệu quả.
- Ví dụ: Trong kỳ 4 tuần (160h chuẩn), nhân viên A nghỉ phép đã duyệt 1 tuần (40h).
  - Giờ khả dụng của A = 160 - 40 = 120 giờ.
  - Nếu A ghi và được duyệt 90 giờ tính phí: Tỷ lệ = (90 / 120) * 100% = 75.0%.
- Trường hợp nhân viên nghỉ phép toàn bộ kỳ (Giờ khả dụng = 0h):
  - Nếu Net Available Hours = 0 và Billable Hours = 0: Tỷ lệ trả về null (hiển thị N/A hoặc 0.0% kèm nhãn nghỉ phép toàn kỳ).

### BR-03. Công thức tính toán theo Bộ phận và Toàn công ty
- Tỷ lệ bộ phận: (Tổng Billable Hours của phòng / Tổng Net Available Hours của phòng) * 100%
- Tỷ lệ toàn công ty: (Tổng Billable Hours toàn công ty / Tổng Net Available Hours toàn công ty) * 100%

### BR-04. Phân quyền và Phạm vi dữ liệu (Data Scope) (NCL-10-CN-002-TC-03)
- VT-01 (Ban Giám Đốc) & VT-06 (Quản trị viên): Phạm vi COMPANY (xem toàn bộ hoặc lọc theo bất kỳ phòng ban/nhân sự nào).
- VT-03 (Quản lý Nguồn lực): Xem theo ORGANIZATION_BRANCH (phòng ban được phân công và các nhánh con).
- Người dùng không có quyền (ví dụ VT-04 Nhân viên, VT-05 Nhân sự):
  - Khi cố gắng truy cập API: Bị từ chối với HTTP Status 403 Forbidden.
  - Hệ thống tự động ghi nhật ký kiểm toán với action ACCESS_DENIED_BILLABLE_HOURS_REPORT hoặc PERMISSION_DENIED.

### BR-05. Nhật ký kiểm toán (Audit Trail) (NCL-10-CN-002-TC-04)
- Mọi thao tác xem báo cáo, đổi bộ lọc phân tích, hoặc xuất báo cáo CSV đều được ghi lại vào bảng audit_logs với đầy đủ:
  - user_id: Người thực hiện
  - action: BILLABLE_HOURS_REPORT_VIEWED hoặc BILLABLE_HOURS_REPORT_EXPORTED
  - details: Kỳ báo cáo, bộ phận, số lượng nhân sự phân tích, thời điểm tạo.

---

## 4. Test Cases chi tiết

| Mã TC | Tiêu đề | Điều kiện đầu vào | Hành động | Kết quả mong đợi |
|---|---|---|---|---|
| NCL-10-CN-002-TC-01 | Luồng thành công | Nhân viên có 120h tính phí trên 160h khả dụng | Xem báo cáo tỷ lệ giờ tính phí | Hệ thống hiển thị chính xác tỷ lệ 75.0% cho nhân viên đó |
| NCL-10-CN-002-TC-02 | Ngoại lệ nghỉ phép | Nhân viên nghỉ phép cả tuần (40h) trong kỳ 160h | Tính tỷ lệ giờ tính phí | Trừ 40h khỏi mẫu số (mẫu số còn 120h), tính tỷ lệ trên mẫu số đã trừ |
| NCL-10-CN-002-TC-03 | Không có quyền | Người dùng vai trò VT-04 (Nhân viên) | Truy cập API / Mở màn hình báo cáo | Từ chối HTTP 403 Forbidden và ghi nhật ký truy cập từ chối vào audit_logs |
| NCL-10-CN-002-TC-04 | Lưu lịch sử kiểm toán | Người dùng hợp lệ xem hoặc xuất báo cáo | Xác nhận xem / tải file | Ghi lại bản ghi audit log với người thực hiện, khoảng tuần, bộ phận và thời điểm |

---

## 5. Thiết kế luồng dữ liệu & API

- API Endpoint:
  - GET /api/v1/reports/billable-rate: Lấy dữ liệu tổng hợp theo toàn công ty, từng bộ phận và từng nhân sự.
  - GET /api/v1/reports/billable-rate/export: Xuất dữ liệu báo cáo chi tiết ra tệp CSV.
- Parameters:
  - orgUnitId (Long, optional): Lọc theo phòng ban.
  - employeeId (Long, optional): Lọc theo nhân viên cụ thể.
  - fromYear, fromWeek (Integer, optional): Tuần bắt đầu.
  - toYear, toWeek (Integer, optional): Tuần kết thúc (Mặc định nếu để trống: Tuần hiện tại; tối đa 104 tuần).