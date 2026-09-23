# Tài liệu Phân tích Nghiệp vụ: Sao lưu và phục hồi dữ liệu

## 1. Tổng quan
- **Mã yêu cầu**: NCL-12-CN-003
- **Tên Use Case**: Sao lưu và phục hồi dữ liệu (System Data Backup and Recovery)
- **Thuộc Epic**: NCL-12 (Quản trị hệ thống & Cấu hình nâng cao)
- **Vai trò thực hiện**: Quản trị viên hệ thống (System Administrator - VT-06)
- **Mục tiêu**: Cung cấp cơ chế sao lưu dữ liệu toàn diện hoặc theo phân hệ kế hoạch nguồn lực (định kỳ tự động hoặc theo yêu cầu thủ công), kiểm tra tính toàn vẹn (checksum SHA-256) và phục hồi an toàn với cơ chế xác nhận 2 bước cùng điểm an toàn tự động, đảm bảo dữ liệu kế hoạch nhiều tháng không bị mất mát khi xảy ra sự cố.

---

## 2. Bối cảnh & Vấn đề Thực tế
- Dữ liệu phân bổ nguồn lực, kế hoạch dự án, năng lực nhân sự, bảng chấm công và kỹ năng được tích lũy qua nhiều tháng/năm, có giá trị chiến lược và khối lượng rất lớn.
- Khi xảy ra lỗi hệ thống, sự cố phần cứng, thao tác nhầm lẫn hoặc tấn công mạng, nếu không có cơ chế sao lưu định kỳ và khôi phục nhanh:
  - Dữ liệu kế hoạch chi tiết không thể phục hồi hoặc tốn hàng tuần để tái thiết.
  - Các dự án bị gián đoạn phân bổ, rủi ro pháp lý và chi phí nhân sự tăng vọt.
- Do đó, hệ thống cần:
  1. Cho phép Admin tạo bản sao lưu tức thời bất kỳ lúc nào.
  2. Lên lịch sao lưu tự động hàng ngày/hàng tuần vào khung giờ thấp điểm.
  3. Kiểm tra tính toàn vẹn và trạng thái bản sao trước khi phục hồi (ngăn chặn phục hồi từ bản sao hỏng/dở dang).
  4. Cơ chế xác nhận 2 bước nghiêm ngặt cùng bản snapshot điểm an toàn trước khi ghi đè dữ liệu.
  5. Ghi nhật ký kiểm toán (Audit Trail) cho toàn bộ hoạt động sao lưu, khôi phục và cả các hành vi truy cập trái phép.

---

## 3. Quy tắc Nghiệp vụ (Business Rules)

### BR-01: Thẩm quyền & Kiểm soát truy cập (RBAC & Permission Scope)
- Chỉ người dùng có vai trò **Quản trị viên (VT-06)** và sở hữu quyền **`DATA_BACKUP_MANAGE`** mới được phép:
  - Xem danh sách bản sao lưu và cấu hình lịch.
  - Tạo mới bản sao lưu thủ công hoặc cấu hình lịch sao lưu tự động.
  - Tải về (download) hoặc tải lên (upload) file sao lưu.
  - Thực hiện khôi phục dữ liệu hoặc xóa bản sao lưu cũ.
- Khi người dùng không thuộc VT-06 cố tình truy cập hoặc gọi API, hệ thống **từ chối ngay lập tức (HTTP 403 Forbidden)** và **tự động ghi nhận nhật ký vi phạm (`ACCESS_DENIED`)** vào bảng `backup_audit_logs`.

### BR-02: Tạo bản sao lưu (On-demand & Scheduled Backup)
- **Tạo theo yêu cầu (Manual)**: Quản trị viên nhập tên bản sao lưu, loại sao lưu (`FULL` - toàn bộ CSDL hoặc `RESOURCE_PLAN` - phân hệ kế hoạch nguồn lực, nhân sự, dự án, chấm công) và ghi chú/mục đích.
- **Tạo theo lịch (Scheduled)**: Hỗ trợ tần suất hàng ngày (`DAILY`) hoặc hàng tuần (`WEEKLY`), cấu hình giờ chạy và thời gian lưu trữ (`retentionDays`).
- **Siêu dữ liệu bản sao lưu**:
  - Mã định danh bản sao lưu (`backupCode` định dạng `BCK-YYYYMMDD-HHMMSS-XXXX`).
  - Dung lượng file chính xác tính bằng Bytes và định dạng hiển thị (KB, MB).
  - Mã băm kiểm tra tính toàn vẹn (SHA-256 Checksum).
  - Trạng thái bản sao: `IN_PROGRESS` (đang xử lý), `COMPLETED` (hoàn tất thành công), `FAILED` (thất bại).
  - Thời điểm tạo (`createdAt`), thời điểm hoàn tất (`completedAt`), người tạo (`createdBy`), cờ tự động (`isAutomatic`).

### BR-03: Kiểm tra trạng thái & Tính toàn vẹn khi phục hồi (Invalid Status & Integrity Check)
- Trước khi thực hiện phục hồi, hệ thống bắt buộc kiểm tra các điều kiện tiên quyết:
  1. **Trạng thái bản sao lưu**: Phải là `COMPLETED`. Nếu bản sao ở trạng thái `FAILED`, `IN_PROGRESS` hoặc bị đánh dấu lỗi, hệ thống từ chối phục hồi với lỗi `InvalidBackupStatusException`.
  2. **File tồn tại & Checksum**: File vật lý phải tồn tại và mã SHA-256 tính toán lại phải khớp 100% với checksum lưu trong siêu dữ liệu. Nếu file bị sửa đổi trái phép hoặc mất mát, từ chối phục hồi.

### BR-04: Xác nhận phục hồi hai bước & Điểm an toàn tự động (Two-Step Restore & Safety Snapshot)
- Phục hồi dữ liệu là thao tác tác động trực tiếp, ghi đè trạng thái CSDL hiện tại về thời điểm của bản sao lưu được chọn.
- Quy trình xác nhận 2 bước bắt buộc:
  - **Bước 1**: Hiển thị bảng tóm tắt chi tiết bản sao lưu nguồn (thời điểm, dung lượng, người tạo, loại sao lưu) kèm cảnh báo nguy hiểm rõ ràng về việc dữ liệu hiện tại sẽ bị thay thế.
  - **Bước 2**: Quản trị viên phải nhập chính xác từ khóa xác nhận `RESTORE` và nhập lý do/giải trình phục hồi (tối thiểu 10 ký tự).
- **Điểm an toàn tự động (Pre-Restore Safety Snapshot)**: Trước khi áp dụng phục hồi, hệ thống tự động sinh một bản sao lưu điểm an toàn có tiền tố `SAFETY_PRE_RESTORE_` để có thể quay lui nếu cần thiết.

### BR-05: Lưu vết kiểm toán toàn diện (Audit Trail Logging)
- Mọi hành vi liên quan đến sao lưu và phục hồi đều được ghi nhận vào `backup_audit_logs`:
  - `action`: `BACKUP_CREATE`, `BACKUP_RESTORE`, `BACKUP_DELETE`, `BACKUP_DOWNLOAD`, `SCHEDULE_UPDATE`, `ACCESS_DENIED`.
  - `user_id`: ID người thực hiện (hoặc người cố tình truy cập trái phép).
  - `backup_id`: ID bản sao lưu liên quan (nếu có).
  - `status`: `SUCCESS`, `FAILED`, `FORBIDDEN`.
  - `reason` & `details`: Chi tiết thay đổi, lý do phục hồi/xóa, kích thước file, checksum.
  - `ip_address`: Địa chỉ IP của máy yêu cầu.
  - `created_at`: Thời điểm chính xác phát sinh thao tác.

---

## 4. Bảng Kịch bản Kiểm thử Nghiệp vụ (Acceptance Criteria)

| Mã kịch bản | Vai trò | Điều kiện tiên quyết | Hành động | Kết quả mong đợi |
|---|---|---|---|---|
| **TC-01** | Quản trị viên (VT-06) | Đăng nhập tài khoản Admin | Tạo bản sao lưu theo yêu cầu với loại `FULL` | Bản sao lưu được tạo thành công, trạng thái `COMPLETED`, lưu dung lượng, mã checksum SHA-256, ghi audit log `BACKUP_CREATE`. |
| **TC-02** | Quản trị viên (VT-06) | Có bản sao lưu hợp lệ (`COMPLETED`) | Chọn phục hồi, hoàn thành xác nhận 2 bước (nhập `RESTORE` + lý do) | Hệ thống tự động tạo snapshot an toàn `SAFETY_PRE_RESTORE_...`, phục hồi CSDL thành công về thời điểm bản sao lưu, ghi audit log `BACKUP_RESTORE`. |
| **TC-03** | Quản trị viên (VT-06) | Có bản sao lưu trạng thái `FAILED` hoặc `IN_PROGRESS` | Thực hiện phục hồi từ bản sao đó | Hệ thống chặn phục hồi, hiển thị thông báo "Bản sao lưu không hợp lệ hoặc chưa hoàn tất", không can thiệp CSDL. |
| **TC-04** | Người dùng thường (VT-04 / VT-02 / VT-05) | Đăng nhập tài khoản không phải VT-06 | Truy cập trang sao lưu hoặc gọi API `/api/v1/backups` | Hệ thống chặn truy cập (HTTP 403 Forbidden), tự động ghi nhật ký vi phạm `ACCESS_DENIED` kèm User ID và IP. |
| **TC-05** | Quản trị viên (VT-06) | Đăng nhập tài khoản Admin | Cấu hình lịch sao lưu tự động (Daily lúc 02:00 sáng, giữ lại 30 ngày) | Cập nhật cấu hình thành công, hiển thị thời điểm chạy tiếp theo (`nextRunAt`), ghi audit log `SCHEDULE_UPDATE`. |
| **TC-06** | Quản trị viên (VT-06) | Đăng nhập tài khoản Admin | Tải về (download) bản sao lưu hoặc tải lên (upload) file hợp lệ | Tải về nhận đúng file kèm checksum; Tải lên được hệ thống xác thực tính hợp lệ, tính checksum và thêm vào danh sách quản lý. |
