# Thiết kế Giao diện NCL-10-CN-002: Báo cáo tỷ lệ giờ tính phí

## 1. Mục tiêu thiết kế
- Cung cấp giao diện trực quan, chuyên nghiệp cho Ban Giám Đốc (VT-01), Quản lý Nguồn lực (VT-03) và Admin (VT-06) theo dõi chỉ số sống còn: **Tỷ lệ giờ tính phí (Billable Utilization Rate)**.
- Đảm bảo thiết kế nhất quán với hệ thống thiết kế chung (Tailwind CSS, Lucide icons, responsive layout, micro-interactions).

## 2. Các thành phần chính của giao diện

### 2.1. Thanh tiêu đề & Công cụ điều khiển (Header & Controls)
- Tiêu đề: **Báo cáo tỷ lệ giờ tính phí**
- Mô tả: *Chỉ số sống còn đo lường hiệu quả sử dụng năng lực và tỷ trọng giờ làm tạo ra doanh thu của toàn công ty, từng phòng ban và từng nhân viên.*
- Nút tác vụ:
  - **Làm mới dữ liệu** (Refresh icon button)
  - **Xuất báo cáo CSV** (Download icon button - kích hoạt /api/v1/reports/billable-rate/export)

### 2.2. Bộ lọc động (Dynamic Filter Bar)
- **Khoảng thời gian**:
  - Từ tuần (Năm / Tuần ISO)
  - Đến tuần (Năm / Tuần ISO)
  - Các nút chọn nhanh (Presets): *Tuần này*, *4 tuần gần nhất*, *Tháng này*, *Quý này*.
- **Phòng ban**: Dropdown chọn toàn công ty hoặc lọc cụ thể một phòng ban (phù hợp theo Data Scope của người dùng).
- **Nhân sự**: Dropdown lọc nhanh theo nhân sự cụ thể.

### 2.3. Thẻ chỉ số tổng quan (KPI Summary Cards)
1. **Tỷ lệ giờ tính phí toàn công ty**: Hiển thị phần trăm nổi bật (ví dụ: 75.0%), thanh đo năng suất (Progress bar màu Indigo/Emerald), so sánh với chuẩn công ty.
2. **Tổng giờ tính phí (Billable)**: Số giờ công có tính phí đã duyệt (1,200.0h).
3. **Tổng giờ khả dụng (Net Available)**: Số giờ làm việc thực tế sau khi trừ lễ và nghỉ phép (1,600.0h).
4. **Tổng giờ không tính phí (Non-billable)**: Giờ đào tạo, nghiên cứu, nội bộ (150.0h).
5. **Quy mô nhân sự**: Tổng số nhân sự tham gia tính toán trong kỳ.

### 2.4. Khu vực phân tích đa chiều (Multi-dimensional Breakdown Tabs)
- **Tab 1: Phân tích theo Phòng ban (Department Breakdown)**
  - Thẻ/Bảng so sánh từng phòng ban: Tên phòng, Số lượng nhân sự, Giờ khả dụng, Giờ tính phí, Tỷ lệ tính phí (Badge & thanh tiến độ màu).
- **Tab 2: Chi tiết theo Từng nhân viên (Employee Detailed Table)**
  - Bảng chi tiết: Mã NV, Họ và tên, Phòng ban, Giờ chuẩn, Giờ nghỉ phép đã duyệt, Giờ khả dụng ròng, Giờ tính phí, Giờ không tính phí, Tỷ lệ giờ tính phí (Badge trực quan), Trạng thái.
- **Tab 3: Biểu đồ trực quan (Visual Charts)**
  - Biểu đồ cột so sánh tương quan giữa Giờ khả dụng vs Giờ tính phí của từng phòng ban.
  - Phân bổ giờ làm việc theo loại.

### 2.5. Xử lý các trạng thái đặc biệt
- **Loading State**: Skeleton loading mượt mà cho thẻ KPI và bảng dữ liệu.
- **Empty State**: Minh họa và thông báo khi chưa có dữ liệu chấm công được duyệt trong kỳ.
- **Forbidden State (403)**: Hiển thị cảnh báo không có quyền truy cập khi tài khoản không thuộc VT-01/VT-03/VT-06.