# Tài liệu Phân tích Nghiệp vụ: Trừ năng lực khả dụng khi nghỉ phép được duyệt

## 1. Tổng quan
- **Mã yêu cầu**: NCL-05-CN-003 / NCL-06-CN-007
- **Vai trò**: Quản lý nguồn lực (Resource Manager - VT-03), Quản lý dự án (Project Manager - VT-02)
- **Mục tiêu**: Tự động trừ số giờ nghỉ phép khỏi giờ khả dụng của nhân sự theo từng tuần ngay khi đơn xin nghỉ phép được **phê duyệt** (`APPROVED`), giúp ma trận năng lực tuần luôn phản ánh đúng thực tế, ngăn ngừa phân bổ trùng/vượt tải cho người đang nghỉ phép.

---

## 2. Quy tắc Nghiệp vụ (Business Rules)

### BR-01: Công thức tính năng lực khả dụng ròng (Net Available Hours)
- Số giờ khả dụng ròng trong tuần (`netAvailableHours`) được tính theo công thức:
  $$\text{netAvailableHours} = \text{standardHours} - \text{holidayHours} - \text{approvedLeaveHours}$$
- Trong đó:
  - `standardHours`: Số giờ làm việc chuẩn trong tuần của nhân sự (mặc định 40h/tuần = 5 ngày x 8h/ngày).
  - `holidayHours`: Số giờ nghỉ lễ trùng vào ngày làm việc chuẩn trong tuần.
  - `approvedLeaveHours`: Tổng số giờ nghỉ phép đã được **phê duyệt** (`APPROVED`) của nhân sự rơi vào các ngày làm việc chuẩn trong tuần đó.

### BR-02: Nguyên tắc xử lý đơn nghỉ phép theo trạng thái
- **Đơn nghỉ phép ở trạng thái `APPROVED` (Đã duyệt)**: Hệ thống lập tức quy đổi số ngày/giờ nghỉ thành `approvedLeaveHours` của các tuần tương ứng và trừ trực tiếp vào số giờ khả dụng của tuần đó.
- **Đơn nghỉ phép ở trạng thái `PENDING` (Chờ duyệt)**: Hệ thống **KHÔNG** trừ số giờ nghỉ vào giờ khả dụng của tuần. Giờ khả dụng của tuần giữ nguyên như trước khi tạo đơn.

### BR-03: Tự động tính toán lại và cảnh báo quá tải cho Quản lý dự án (PM)
- Ngay sau khi đơn nghỉ phép được duyệt, hệ thống tính toán lại cờ quá tải cho tất cả các tuần bị ảnh hưởng:
  $$\text{isOverloaded} = (\text{allocatedHours} > \text{netAvailableHours})$$
- Nếu nhân sự đã được phân bổ công việc cho một hoặc nhiều dự án trong tuần đó với tổng `allocatedHours > netAvailableHours`:
  1. Đánh dấu cờ quá tải `is_overloaded = true` cho các bản ghi phân bổ dự án trong tuần.
  2. Tạo cảnh báo quá tải (Overload Alert) gửi/hiển thị cho Quản lý dự án (PM) của các dự án có phân bổ nhân sự đó trong tuần quá tải.
  3. Ghi nhận nhật ký kiểm toán cảnh báo quá tải tự động do nghỉ phép.

### BR-04: Phân quyền truy cập & Ghi nhật ký từ chối truy cập (Access Refusal Log)
- Xem bảng năng lực khả dụng theo tuần yêu cầu vai trò Quản lý nguồn lực (VT-03), Admin/Lãnh đạo (VT-01), hoặc PM (VT-02) trong phạm vi dữ liệu (Data Scope) cho phép.
- Nếu người dùng **KHÔNG** có vai trò/quyền hợp lệ truy cập chức năng xem năng lực khả dụng theo tuần:
  1. Hệ thống từ chối truy cập (Trả về lỗi HTTP `403 Forbidden` / `PermissionDeniedException`).
  2. Hệ thống tự động ghi lại nhật ký từ chối truy cập (`ACCESS_DENIED_CAPACITY_VIEW`) bao gồm: ID người dùng (`user_id`), vai trò cố gắng truy cập, hành động và thời điểm hệ thống từ chối.

### BR-05: Nhật ký kiểm toán thay đổi năng lực khi nghỉ phép (Audit Trail)
- Mỗi khi đơn nghỉ phép được duyệt làm thay đổi số giờ khả dụng của nhân sự:
  - Ghi nhật ký kiểm toán `LEAVE_CAPACITY_DEDUCTED` lưu thông tin: người thực hiện duyệt (`currentUserId`), mã đơn nghỉ phép, ID nhân sự, số giờ trừ, các tuần bị ảnh hưởng, và thời điểm thực hiện.

---

## 3. Các kịch bản Kiểm thử Nghiệp vụ (Acceptance Criteria / Use Cases)

| Mã | Kịch bản / Điều kiện ban đầu | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| **TC-01** | Nhân sự có 40h khả dụng trong tuần | Duyệt đơn nghỉ 2 ngày (16h) của tuần đó | Giờ khả dụng của tuần giảm từ 40h còn 24h. |
| **TC-02** | Nhân sự đã được phân bổ 32h trong tuần đó | Duyệt đơn nghỉ 2 ngày (khả dụng giảm còn 24h) | Hệ thống đánh dấu tuần đó quá tải (32h > 24h, vượt 8h) và tạo cảnh báo cho Quản lý dự án (PM) liên quan. |
| **TC-03** | Đơn nghỉ mới chỉ đang ở trạng thái chờ duyệt (`PENDING`) | Tính giờ khả dụng của tuần | Hệ thống chưa trừ giờ của đơn chưa được duyệt. Giờ khả dụng vẫn là 40h. |
| **TC-04** | Người dùng không phải RM, PM trong scope hoặc nhân sự xem chính mình | Mở chức năng xem năng lực khả dụng theo tuần | Hệ thống từ chối truy cập (403) và ghi nhật ký lần từ chối (`ACCESS_DENIED_CAPACITY_VIEW`). |
| **TC-05** | Có thay đổi liên quan tới trừ năng lực khả dụng khi nghỉ phép | Phê duyệt thành công đơn nghỉ phép | Hệ thống ghi lại nhật ký kiểm toán chứa người thực hiện, nội dung thay đổi, ID nhân sự, số giờ trừ và thời điểm. |

---

## 4. Xử lý Trường hợp Ngoại lệ (Edge Cases)
1. **Nghỉ phép vắt qua nhiều tuần**: Đơn nghỉ phép kéo dài qua 2 hoặc nhiều tuần chuẩn -> Hệ thống bóc tách chính xác số giờ nghỉ phép rơi vào ngày làm việc của từng tuần và cập nhật năng lực cho từng tuần tương ứng.
2. **Đơn nghỉ phép bị hủy sau khi đã duyệt**: Nếu đơn nghỉ đã duyệt bị hủy (`CANCELLED`), hệ thống tự động hoàn trả số giờ nghỉ phép và tính toán lại năng lực khả dụng cho các tuần bị ảnh hưởng.
3. **Hợp đồng lao động hết hạn giữa tuần**: Nếu hợp đồng nhân sự kết thúc trong tuần, số giờ khả dụng được điều chỉnh theo hợp đồng trước khi trừ giờ nghỉ phép.
