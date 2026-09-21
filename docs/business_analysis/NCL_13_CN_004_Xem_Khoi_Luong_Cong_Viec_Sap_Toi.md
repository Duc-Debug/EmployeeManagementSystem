# Kế hoạch triển khai NCL-13-CN-004: Xem khối lượng công việc sắp tới của tôi (Upcoming Workload View)

## 1. Thông tin chung

- **Tên chức năng**: Xem khối lượng công việc sắp tới của tôi
- **Mã User Story**: NCL-13-CN-004
- **Epic**: NCL-13 - Cổng tương tác nhân viên & Lịch cá nhân
- **Vai trò chính**: Nhân viên chuyên môn (VT-04)
- **Vai trò được xem**: Nhân viên chuyên môn (VT-04 xem của chính mình), Quản lý nguồn lực (VT-03 xem nhân sự phòng ban), Quản lý dự án (VT-02), Ban giám đốc (VT-01), Quản trị viên (VT-06)
- **Độ ưu tiên**: Cao (High Priority)
- **Chu kỳ**: Chu kỳ kế hoạch

---

## 2. Mục tiêu nghiệp vụ

Là một **Nhân viên chuyên môn (VT-04)**, tôi muốn nhìn thấy mức bận (tải công việc) của mình trong 8 tuần tới để:
1. Nắm bắt trực quan các dự án và số giờ mình được phân bổ trong từng tuần.
2. Tự nhận ra nguy cơ quá tải (Overload) trước cả khi hệ thống hoặc cấp trên cảnh báo để chủ động sắp xếp công việc hoặc thương lượng điều chỉnh.
3. Nhận biết các tuần nhàn rỗi (Underutilized) để sẵn sàng nhận thêm việc hoặc chủ động đăng ký đào tạo / nâng cao kỹ năng.

---

## 3. Quy tắc tính toán & Nghiệp vụ chi tiết

### BR-01. Phạm vi thời gian mô phỏng (8 tuần liên tiếp)
- Hệ thống hiển thị mặc định **8 tuần liên tiếp** (ISO-8601) bắt đầu từ tuần hiện tại (hoặc tuần được chọn `fromYear`/`fromWeek`).
- Xử lý mượt mà chuyển giao năm dương lịch (ví dụ từ tuần 50 năm nay sang các tuần đầu năm sau).

### BR-02. Công thức tính toán năng lực khả dụng ròng (Net Available Hours)
- Tại mỗi tuần $W_i$:
  $$\text{Net Available Hours} = \max(0, \text{Standard Hours} - \text{Holiday Hours} - \text{Approved Leave Hours})$$
  - $\text{Standard Hours}$: Giờ làm việc chuẩn của nhân viên trong tuần (mặc định 40h hoặc cấu hình riêng theo hợp đồng).
  - $\text{Holiday Hours}$: Số giờ nghỉ lễ công ty trong tuần (theo Lịch làm việc công ty).
  - $\text{Approved Leave Hours}$: Tổng số giờ nghỉ phép đã được duyệt (`status = 'APPROVED'`) trong tuần đó.

### BR-03. Công thức tính khối lượng công việc & Tỷ lệ sử dụng năng lực (% Workload Utilization)
- $\text{Allocated Hours}$: Tổng số giờ phân bổ từ các dự án đang chạy trong tuần ($W_i$) từ bảng `weekly_project_allocations`.
- $\text{Utilization Percentage} (\%):$
  $$\text{Utilization} (\%) = \begin{cases} 
  (\text{Allocated Hours} / \text{Net Available Hours}) \times 100\% & \text{khi Net Available Hours} > 0 \\
  100\%+ \text{ (Quá tải)} & \text{khi Net Available Hours} = 0 \text{ và Allocated Hours} > 0 \\
  0.0\% & \text{khi Net Available Hours} = 0 \text{ và Allocated Hours} = 0
  \end{cases}$$

### BR-04. Đánh giá trạng thái theo Ngưỡng Quá tải và Nhàn rỗi (QTN-23)
Hệ thống lấy cấu hình ngưỡng hiệu lực từ `CapacityThresholdService` (hoặc fallback mặc định):
- $\text{Overload Threshold}$ (mặc định $100.0\%$):
  - Khi $\text{Utilization} > \text{Overload Threshold} \rightarrow$ Trạng thái **`OVERLOADED`** (Quá tải).
  - Tô màu cảnh báo Đỏ/Hồng.
  - Tính số giờ vượt: $\text{Overload Hours} = \max(0, \text{Allocated Hours} - (\text{Net Available Hours} \times \text{Overload Threshold} / 100))$.
- $\text{Idle Threshold}$ (mặc định $70.0\%$):
  - Khi $\text{Utilization} < \text{Idle Threshold} \rightarrow$ Trạng thái **`IDLE`** (Nhàn rỗi / Tải thấp).
  - Tô màu Vàng/Hổ phách.
- Khi $\text{Idle Threshold} \le \text{Utilization} \le \text{Overload Threshold} \rightarrow$ Trạng thái **`NORMAL`** (Bình thường / Cân bằng).
  - Tô màu Xanh lục / Xanh lam.

### BR-05. Phân rã theo từng dự án (Project Breakdown)
- Trong mỗi tuần, hiển thị chi tiết danh sách các dự án tham gia: Tên dự án, mã dự án, vai trò đảm nhiệm (`projectRoleName`), số giờ được phân bổ (`allocatedHours`), tỷ lệ % phân bổ (`allocationPercentage`).

### BR-06. Phân quyền và Phạm vi Dữ liệu (Data Scope) (NCL-13-CN-004-TC-03)
- `VT-04` (Nhân viên): Chỉ được xem khối lượng công việc của chính mình (`SELF` scope). Nếu cố tình truyền `employeeId` của nhân viên khác, hệ thống chặn với HTTP 403 Forbidden và ghi log từ chối.
- `VT-03` (RM): Xem nhân viên trong phòng ban quản lý (`ORGANIZATION_BRANCH`).
- `VT-01`, `VT-05`, `VT-06`: Xem toàn công ty (`COMPANY`).

### BR-07. Nhật ký kiểm toán (Audit Trail) (NCL-13-CN-004-TC-04)
- Ghi nhật ký vào bảng `audit_logs`:
  - `MY_WORKLOAD_VIEWED`: Ghi nhận người dùng xem khối lượng công việc 8 tuần của mình.
  - `ACCESS_DENIED_EMPLOYEE_WORKLOAD`: Ghi nhận các trường hợp cố ý truy cập dữ liệu nhân sự ngoài phạm vi cho phép.

---

## 4. Đặc tả Test Cases

| Mã Test Case | Tên kịch bản | Điều kiện đầu vào | Các bước thực hiện | Kết quả mong đợi |
| :--- | :--- | :--- | :--- | :--- |
| **NCL-13-CN-004-TC-01** | Luồng thành công xem 8 tuần | Nhân viên đã có phân bổ trong 8 tuần tới | 1. Mở biểu đồ khối lượng công việc.<br>2. Xem kết quả trả về. | Hiển thị 8 cột biểu đồ tương ứng 8 tuần với đầy đủ số giờ phân bổ, giờ khả dụng ròng và % tải. |
| **NCL-13-CN-004-TC-02** | Ngoại lệ vượt ngưỡng quá tải | Tuần $W_k$ có phân bổ 48h trên 40h chuẩn (120%) | 1. Mở biểu đồ khối lượng công việc.<br>2. Quan sát tuần $W_k$. | Cột tuần $W_k$ được tô màu Đỏ/Hồng cảnh báo kèm số giờ vượt (+8.0h quá tải). |
| **NCL-13-CN-004-TC-03** | Không có quyền (Data Scope vi phạm) | Nhân viên A gọi API xem khối lượng của Nhân viên B | Gửi yêu cầu với `employeeId = B` từ tài khoản A (`SELF` scope). | Hệ thống trả về `403 Forbidden`, ném `PermissionDeniedException` và ghi Audit Log từ chối. |
| **NCL-13-CN-004-TC-04** | Ghi nhận nhật ký kiểm toán | Người dùng mở xem khối lượng công việc | Thực hiện gọi API `GET /api/v1/workload/my-upcoming` | Hệ thống lưu bản ghi kiểm toán với `action = MY_WORKLOAD_VIEWED`. |
