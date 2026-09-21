# Tài liệu Phân tích Nghiệp vụ: Cấu hình kênh và tần suất nhận thông báo

## 1. Tổng quan
- **Mã yêu cầu**: NCL-11-CN-002
- **Tên Use Case**: Cấu hình kênh và tần suất nhận thông báo (Notification Channels and Frequency Configuration / Notification Preferences)
- **Thuộc Epic**: NCL-11 (Thông báo và nhắc việc)
- **Vai trò áp dụng**: Mọi người dùng đã xác thực (`VT-01` $\rightarrow$ `VT-06` theo `docs/ROLE_BASED_ACCESS_CONTROL_GUIDE.md` Mục 3)
- **Mục tiêu**: Cho phép người dùng chủ động cấu hình kênh nhận thông báo (trên ứng dụng / Email) cho từng loại sự kiện, chọn tần suất nhận (tức thời, tổng hợp hàng ngày, hàng tuần), tùy chỉnh số ngày nhắc việc sắp đến hạn (`TASK_DUE_REMINDER`) và thiết lập khung giờ yên tĩnh ngoài giờ làm việc nhằm tối ưu hóa trải nghiệm làm việc và tránh quá tải thông tin.

---

## 2. Bối cảnh & Vấn đề Thực tế
- Trong hệ thống "Kế Hoạch Nguồn Lực", các hoạt động điều phối nhân sự theo tuần diễn ra liên tục:
  1. Thay đổi phân bổ nhân sự vào dự án & cảnh báo quá tải / xung đột lịch tuần (`QTN-11`, `QTN-15`, `NCL-07`).
  2. Giao việc mới và nhắc việc sắp đến hạn (`NCL-04`, `NCL-11-CN-004`).
  3. Nhắc nhở nộp và duyệt bảng chấm công theo tuần (`NCL-09`).
  4. Thảo luận công việc và nhắc tên (`TASK_COMMENT`, `TASK_MENTION`).
- Nếu không có chức năng cấu hình kênh và tần suất:
  - Tất cả người dùng đều nhận thông báo mặc định khiến hòm thư email bị spam hoặc trung tâm thông báo bị ngợp.
  - Ngược lại, những cảnh báo cấp thiết về vỡ kế hoạch phân bổ hay xung đột lịch có thể bị bỏ lỡ nếu không được đẩy qua kênh email ngay lập tức.
  - Người dùng không có khả năng chọn thời gian nhắc việc trước hạn chót phù hợp với nhịp làm việc cá nhân (ví dụ: 1 ngày, 2 ngày, 3 ngày hoặc 7 ngày).

---

## 3. Quy tắc Nghiệp vụ (Business Rules)

### BR-01: Quyền hạn & Tính riêng tư (Self-service Preference)
- Mỗi người dùng chỉ được quyền truy xuất và cập nhật cấu hình thông báo của chính tài khoản của mình.
- Hệ thống tự động khởi tạo cấu hình mặc định an toàn cho người dùng mới khi truy cập lần đầu.

### BR-02: Các Kênh Thông báo (Channels)
- `IN_APP`: Hiển thị trong Trung tâm thông báo (Notification Popover) trên thanh công cụ.
- `EMAIL`: Gửi qua hòm thư điện tử của người dùng.
- Có hỗ trợ bật/tắt tổng thể từng kênh (`emailEnabled`, `inAppEnabled`).

### BR-03: Ràng buộc Kênh Cảnh báo Trọng yếu (Critical Alert Channel Constraint)
- Đối với các sự kiện có mức độ `CAO` hoặc ảnh hưởng trực tiếp tới kế hoạch dự án (`SCHEDULE_CONFLICT` - xung đột lịch, `ALLOCATION_CHANGED` - thay đổi phân bổ), hệ thống **bắt buộc phải bật ít nhất một kênh nhận thông báo** (In-App hoặc Email). Người dùng không được phép tắt đồng thời cả hai kênh đối với nhóm này (`NONE` bị chặn).

### BR-04: Tần suất Nhận Thông báo (Notification Frequency)
- Hỗ trợ 3 chế độ tần suất:
  1. `IMMEDIATE`: Gửi tức thời ngay khi sự kiện phát sinh.
  2. `DAILY_DIGEST`: Tổng hợp các thông báo trong ngày vào bản tin tóm tắt.
  3. `WEEKLY_DIGEST`: Tổng hợp thông báo tuần vào đầu tuần.
- Tần suất nhắc việc sắp đến hạn (`taskDueReminderDays`): Cho phép chọn `1`, `2`, `3`, `5`, `7` ngày trước hạn chót (Mặc định: `3` ngày).

### BR-05: Khung giờ yên tĩnh (Quiet Hours)
- Người dùng có thể bật chế độ yên tĩnh và thiết lập khung giờ (ví dụ: từ `22:00` đến `07:00`).
- Trong khung giờ yên tĩnh, các thông báo thông thường sẽ được hoãn gửi email để tôn trọng thời gian nghỉ ngơi của nhân sự.

### BR-06: Khôi phục cấu hình mặc định (Reset to Default)
- Người dùng có thể khôi phục toàn bộ cấu hình về trạng thái mặc định của hệ thống bất kỳ lúc nào:
  - `inAppEnabled`: `true`, `emailEnabled`: `true`.
  - Mọi sự kiện: `ALL` (Cả 2 kênh).
  - Tần suất: `IMMEDIATE`.
  - Nhắc việc: `3` ngày trước deadline.
  - Khung giờ yên tĩnh: `false`.

---

## 4. Các kịch bản Kiểm thử Nghiệp vụ (Acceptance Criteria)

| Mã | Tiền điều kiện | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| **TC-01** | Người dùng đã đăng nhập chưa từng lưu cấu hình. | Gửi yêu cầu `GET /api/v1/notification-preferences/me` | Thành công: Nhận được cấu hình mặc định đầy đủ (In-app: true, Email: true, frequency: IMMEDIATE, reminder: 3 ngày). |
| **TC-02** | Người dùng đã đăng nhập. | Cập nhật cấu hình: tắt email cho trao đổi (`taskCommentChannel = IN_APP_ONLY`), đổi số ngày nhắc việc thành 5 ngày | Thành công (HTTP 200), cấu hình được lưu bền vững vào CSDL, version tăng lên. |
| **TC-03** | Người dùng đã đăng nhập. | Cố gắng tắt cả 2 kênh nhận cho cảnh báo xung đột lịch (`scheduleConflictChannel = NONE`) | Bị chặn với lỗi nghiệp vụ `NotificationPreferenceValidationException` (vi phạm BR-03). |
| **TC-04** | Người dùng đã đăng nhập. | Cập nhật số ngày nhắc việc không hợp lệ (ví dụ: 0 ngày hoặc 30 ngày) | Bị từ chối do vượt ngưỡng hợp lệ [1..14] ngày. |
| **TC-05** | Người dùng đã tùy chỉnh nhiều thiết lập. | Gửi yêu cầu `POST /api/v1/notification-preferences/me/reset` | Thành công: Tất cả các trường được đưa về giá trị chuẩn mặc định của hệ thống. |
| **TC-06** | Chưa đăng nhập (không có JWT token). | Gọi API cấu hình thông báo | Bị từ chối HTTP 401 Unauthorized. |
