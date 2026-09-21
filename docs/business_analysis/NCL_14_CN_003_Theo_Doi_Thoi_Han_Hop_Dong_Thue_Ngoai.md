# Tài liệu Phân tích Nghiệp vụ: Theo dõi thời hạn hợp đồng thuê ngoài

## 1. Thông tin chung
- **Mã User Story**: NCL-14-CN-003
- **Tên User Story**: Theo dõi thời hạn hợp đồng thuê ngoài (Monitor Outsourced Contract Expiration)
- **Thuộc Epic**: NCL-14 (Năng lực thuê ngoài)
- **Vai trò chính**: Quản lý nguồn lực (Resource Manager - VT-03)
- **Vai trò phối hợp**: Nhân sự (HR Specialist - VT-05)
- **Mục tiêu**: Hệ thống tự động rà soát thời hạn các hợp đồng thuê ngoài và cảnh báo trước 30 ngày (ví dụ còn 25 ngày theo TC-01), đồng thời chỉ ra chính xác các phân bổ dự án vắt qua ngày hết hạn hoặc nằm ngoài thời hạn hợp đồng để Quản lý nguồn lực kịp thời gia hạn hoặc tìm nhân sự thay thế, tránh gián đoạn dự án.

---

## 2. Bối cảnh & Vấn đề Thực tế
- Nhân sự thuê ngoài (Outsource / Contractor) là nguồn lực tạm thời bổ sung vào dự án nhưng luôn bị ràng buộc bởi thời hạn hợp đồng dịch vụ.
- Nếu không có cơ chế chủ động theo dõi và cảnh báo trước:
  1. Hợp đồng hết hạn đột ngột khiến nhân sự phải dừng công việc, dự án bị "mất người giữa chừng", gây trễ tiến độ.
  2. Kế hoạch phân bổ tuần vẫn duy trì cho các tuần sau ngày hợp đồng hết hạn, dẫn đến vi phạm nghiêm ngặt quy tắc nghiệp vụ **QTN-21**.
  3. Quản lý nguồn lực và Nhân sự bị động trong việc đàm phán gia hạn với nhà cung cấp hoặc tìm người thay thế.

---

## 3. Quy tắc Nghiệp vụ cốt lõi (Business Rules)

### BR-01: Quy tắc phân bổ trong hạn hợp đồng (QTN-21)
- Nhân sự thuê ngoài chỉ được phân bổ vào các tuần nằm trong thời hạn hợp đồng thuê.
- Khi rà soát, hệ thống phát hiện và phân loại các phân bổ vi phạm/có nguy cơ:
  - **`SPANS_OVER_EXPIRY` (Vắt qua ngày hết hạn)**: Tuần phân bổ chứa ngày hết hạn hợp đồng (`weekStartDate <= contractEndDate < weekEndDate`). Do hợp đồng kết thúc ở giữa tuần, các ngày làm việc sau ngày hết hạn trong tuần đó không còn hiệu lực.
  - **`AFTER_EXPIRY` (Sau ngày hết hạn)**: Tuần phân bổ bắt đầu hoàn toàn sau ngày hết hạn (`weekStartDate > contractEndDate`). Đây là phân bổ ngoài hạn hợp đồng.

### BR-02: Quy tắc chống gửi trùng lặp thông báo (QTN-19)
- Hệ thống quét rà soát định kỳ hàng ngày (Scheduled Scanner lúc 06:00 AM) hoặc khi Quản lý nguồn lực / Nhân sự bấm nút rà soát thủ công.
- Mỗi sự kiện hợp đồng sắp hết hạn chỉ gửi một thông báo duy nhất cho mỗi người nhận.
- Áp dụng `source_event_key = OUTSOURCED_CONTRACT_EXPIRY_{employeeId}_{contractEndDate}` qua `CreateNotificationEventUseCase`. Các lần rà soát sau sẽ không gửi lại thông báo trùng lặp nếu ngày hết hạn hợp đồng chưa thay đổi.

### BR-03: Ngưỡng cảnh báo trước 30 ngày
- Hợp đồng được xếp vào nhóm cảnh báo khi:
  - `0 <= daysRemaining <= 30` (Trạng thái: `EXPIRING_SOON`).
  - `daysRemaining < 0` (Trạng thái: `EXPIRED` - đã quá hạn nhưng vẫn còn phân bổ tồn đọng).

### BR-04: Phân quyền truy cập (RBAC - TC-03)
- Chỉ người dùng có vai trò `VT-03` (Quản lý nguồn lực) hoặc `VT-05` (Nhân sự) mới được phép mở chức năng, tra cứu và kích hoạt rà soát thời hạn hợp đồng thuê ngoài.
- Mọi truy cập từ vai trò khác (ví dụ `VT-04` Nhân viên chuyên môn) đều bị chặn (HTTP 403 Forbidden / `PermissionDeniedException`) VÀ **bắt buộc ghi nhận nhật ký lần từ chối** vào bảng `audit_logs` với `action = 'ACCESS_DENIED'`, `table_name = 'OUTSOURCED_CONTRACT_EXPIRATION'`.

### BR-05: Lưu vết kiểm toán thao tác (Audit Trail - TC-04)
- Khi Quản lý nguồn lực hoặc Nhân sự thực hiện thao tác liên quan tới theo dõi thời hạn hợp đồng (chạy rà soát thủ công, xác nhận ghi nhận/xử lý cảnh báo), hệ thống tự động ghi bản ghi vào bảng `audit_logs` gồm:
  - `user_id`: Người thực hiện
  - `action`: `'MANUAL_SCAN'` hoặc `'ACKNOWLEDGE_WARNING'`
  - `table_name`: `'OUTSOURCED_CONTRACT_EXPIRATION'`
  - `record_id`: ID nhân viên thuê ngoài
  - `old_value`, `new_value`: Chi tiết nội dung và ghi chú xử lý
  - `created_at`: Thời điểm thao tác

---

## 4. Các kịch bản Kiểm thử Nghiệp vụ (Acceptance Criteria)

| Mã tiêu chí | Loại trường hợp | Điều kiện ban đầu (Given) | Hành động (When) | Kết quả mong đợi (Then) | Dữ liệu kiểm thử |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NCL-14-CN-003-TC-01** | Gửi thông báo | Hợp đồng thuê còn 25 ngày là hết hạn (trong ngưỡng <= 30 ngày) | Hệ thống chạy rà soát | Quản lý nguồn lực nhận cảnh báo kèm các phân bổ vắt qua ngày hết hạn | Nhân sự thuê ngoài, thời hạn hợp đồng, danh sách phân bổ tuần |
| **NCL-14-CN-003-TC-02** | Dữ liệu rỗng | Không có hợp đồng thuê nào sắp hết hạn | Hệ thống chạy rà soát | Hệ thống không gửi cảnh báo nào, trả kết quả rỗng an toàn | Hợp đồng còn dài hạn (> 30 ngày) hoặc không có nhân sự thuê ngoài |
| **NCL-14-CN-003-TC-03** | Không có quyền | Người dùng không phải Quản lý nguồn lực (`VT-03`) hoặc Nhân sự (`VT-05`) | Mở chức năng theo dõi thời hạn hợp đồng thuê ngoài | Hệ thống từ chối truy cập (HTTP 403) và ghi nhật ký lần từ chối vào `audit_logs` | Vai trò người dùng (VT-04) |
| **NCL-14-CN-003-TC-04** | Lưu lịch sử | Có thay đổi liên quan tới theo dõi thời hạn hợp đồng thuê ngoài | Xác nhận thao tác | Hệ thống ghi lại người thực hiện, nội dung và thời điểm vào `audit_logs` | Thao tác rà soát / xác nhận ghi nhận cảnh báo |

---

## 5. Đặc tả API Backend (RESTful)

1. `GET /api/outsourced-contracts/expiring`
   - Mục đích: Lấy danh sách hợp đồng thuê ngoài sắp hết hạn trong 30 ngày kèm chi tiết phân bổ vắt qua ngày hết hạn.
   - Quyền: `VT-03`, `VT-05`.
   - Query Param: `thresholdDays` (default = 30).
2. `POST /api/outsourced-contracts/scan`
   - Mục đích: Kích hoạt rà soát và gửi cảnh báo tới Quản lý nguồn lực (VT-03) qua Notification Center.
   - Quyền: `VT-03`, `VT-05`.
3. `POST /api/outsourced-contracts/{employeeId}/acknowledge`
   - Mục đích: Xác nhận xử lý cảnh báo hợp đồng, ghi nhận audit log theo TC-04.
   - Quyền: `VT-03`, `VT-05`.
