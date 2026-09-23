# Tài liệu Phân tích Nghiệp vụ: Điều chỉnh giờ làm đã duyệt

## 1. Tổng quan
- **Mã yêu cầu**: NCL-09-CN-005
- **Tên Use Case**: Điều chỉnh giờ làm đã duyệt (Adjust Approved Work Log / Timesheet Entry)
- **Thuộc Epic**: NCL-09 (Giờ làm thực tế và đối chiếu kế hoạch)
- **Vai trò**: Quản lý dự án (Project Manager - VT-02), Quản trị viên (Admin - VT-06), Nhân viên chuyên môn (VT-04 - tra cứu vết)
- **Mục tiêu**: Cho phép Quản lý dự án (PM) hoặc Quản trị viên điều chỉnh lại số giờ làm, công việc hoặc mô tả của một dòng ghi giờ đã được duyệt (`APPROVED`) khi phát hiện sai sót, với điều kiện bắt buộc phải ghi nhận lý do giải trình và lưu vết kiểm toán đầy đủ (`timesheet_audit_logs`), đồng thời tự động cập nhật lại tổng giờ tuần và cảnh báo ngân sách công việc.

---

## 2. Bối cảnh & Vấn đề Thực tế
- Ở các đơn vị làm việc theo dự án, sau khi PM đã duyệt giờ công tuần của nhân sự, thực tế có thể phát sinh các trường hợp:
  1. Nhân sự ghi nhầm mã công việc (Task) hoặc nhầm dự án.
  2. Nhân sự nhập sai số giờ (ví dụ nhầm 4h thành 8h, hoặc quên ghi giờ làm thêm OT).
  3. Sau buổi đối chiếu tiến độ hàng tuần, PM và nhân viên thống nhất điều chỉnh lại giờ thực tế cho đúng khối lượng hoàn thành.
- Nếu không có chức năng điều chỉnh giờ đã duyệt:
  - Bảng chấm công bị khóa cứng, dẫn tới dữ liệu giờ thực tế (`actualHours`) bị sai lệch vĩnh viễn.
  - Báo cáo chênh lệch kế hoạch vs thực tế (Timesheet Variance Report) mất độ tin cậy, làm sai lệch phân tích năng lực tuần và kế hoạch nhận dự án mới.

---

## 3. Quy tắc Nghiệp vụ (Business Rules)

### BR-01: Thẩm quyền điều chỉnh (Permission & Scope)
- Chỉ người dùng có quyền `WORK_LOG_ADJUST` hoặc `WORK_LOG_APPROVE` VÀ là **Quản lý dự án đang phụ trách dự án đó** (`project.isManagedBy(currentEmployeeId)`), hoặc là **Quản trị viên (Admin)** mới được phép thực hiện điều chỉnh dòng giờ công đã duyệt.
- Nhân viên sở hữu không được tự ý sửa dòng đã duyệt.

### BR-02: Bắt buộc giải trình lý do (Mandatory Justification Reason)
- Bất kỳ thao tác điều chỉnh dòng giờ công đã duyệt nào đều **bắt buộc** phải cung cấp lý do giải trình (`reason`).
- Lý do không được để trống và phải có độ dài tối thiểu 10 ký tự.

### BR-03: Kiểm tra tính hợp lệ của dữ liệu (Validation Constraints)
- **Trạng thái hợp lệ**: Chỉ áp dụng cho các dòng giờ công đang ở trạng thái `APPROVED`. Nếu dòng ở trạng thái khác (`DRAFT`, `SUBMITTED`, `REJECTED`), hệ thống từ chối bằng lỗi nghiệp vụ tương ứng.
- **Giới hạn giờ**: Số giờ điều chỉnh mới phải $> 0$ và $\le 24$ giờ.
- **Giới hạn 12 giờ/ngày (QTN-09)**: Tổng số giờ làm việc thực tế trong ngày đó của nhân sự (sau khi cộng trừ chênh lệch của dòng điều chỉnh) không được vượt quá 12 giờ/ngày.
- **Ràng buộc kỳ khóa (Period Lock)**: Nếu tuần làm việc của dòng giờ công nằm trong Kỳ phân bổ đã bị Khóa (`AllocationPeriod.LOCKED`), hệ thống từ chối điều chỉnh để đảm bảo tính toàn vẹn số liệu đã chốt.
- **Ràng buộc Task hợp lệ**: Nếu chuyển sang Task mới, Task đó phải thuộc cùng Dự án và không phải là hạng mục cha (`CATEGORY`).

### BR-04: Cập nhật dữ liệu & Tính toán lại (Re-calculation)
- Cập nhật số giờ, công việc, tính phí (`isBillable`), mô tả mới cho `TimesheetEntry`.
- Tự động tính toán lại tổng số giờ làm việc của bảng chấm công tuần (`timesheet.recalculateTotalHours()`).
- Trạng thái dòng giờ công vẫn duy trì là `APPROVED`.
- Tăng số phiên bản `version` (Optimistic Locking) để chống xung đột cập nhật đồng thời.

### BR-05: Lưu vết kiểm toán nghiêm ngặt (Audit Trail)
- Hệ thống tự động ghi một bản ghi kiểm toán vào bảng `timesheet_audit_logs` gồm:
  - `action`: `'ADJUST_APPROVED'`
  - `actor_id`: ID của người dùng thực hiện điều chỉnh
  - `timesheet_id`: ID của bảng chấm công tuần
  - `entry_id`: ID của dòng giờ công
  - `note`: Ghi rõ chi tiết thay đổi, gồm giờ cũ, giờ mới và lý do giải trình. Ví dụ: `[ĐIỀU CHỈNH GIỜ ĐÃ DUYỆT] Giờ cũ: 8.00h -> Giờ mới: 6.00h | Lý do: Nhân viên ghi nhầm giờ họp sprint`

### BR-06: Cảnh báo ngân sách công việc (Task Budget Warning)
- Khi điều chỉnh giờ làm, hệ thống tự động kiểm tra lại tổng giờ thực tế của Task:
  - Nếu đạt từ 80% ngân sách: Cảnh báo tiến độ đạt $\ge 80\%$.
  - Nếu vượt 100% ngân sách: Cảnh báo vượt ngân sách giờ của Task.
- Danh sách cảnh báo được trả về trong phản hồi để PM nắm rõ.

---

## 4. Các kịch bản Kiểm thử Nghiệp vụ (Acceptance Criteria)

| Mã | Tiền điều kiện | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| **TC-01** | Dòng giờ công đã duyệt 8h. Người dùng là PM phụ trách dự án. | Điều chỉnh thành 6h kèm lý do "Bù giờ công tác thực tế" | Thành công: Giờ entry cập nhật thành 6h, tổng giờ tuần giảm 2h, lưu audit log với action `ADJUST_APPROVED`. |
| **TC-02** | Dòng giờ công đã duyệt 4h. Ngày đó nhân sự đã làm 10h ở các task khác. | PM điều chỉnh tăng thành 7h (tổng ngày 13h > 12h) | Bị chặn lỗi `DailyHoursLimitExceededException` (vi phạm QTN-09). |
| **TC-03** | Dòng giờ công đã duyệt. | PM điều chỉnh nhưng để trống lý do hoặc nhập dưới 10 ký tự | Bị chặn lỗi validation `WorkLogAdjustmentReasonRequiredException`. |
| **TC-04** | Dòng giờ công đang ở trạng thái `DRAFT` hoặc `SUBMITTED`. | Gọi API điều chỉnh giờ đã duyệt | Bị từ chối với lỗi `TimesheetNotApprovedException`. |
| **TC-05** | Người dùng không phải PM của dự án và không phải Admin. | Thao tác điều chỉnh dòng đã duyệt | Bị từ chối quyền truy cập (HTTP 403 / `PermissionDeniedException`). |
| **TC-06** | Dòng giờ công thuộc tuần nằm trong Kỳ phân bổ đã bị khóa (`LOCKED`). | Thao tác điều chỉnh dòng đã duyệt | Bị chặn lỗi `AllocationPeriodLockedException`. |
| **TC-07** | Dòng giờ công có version cũ hơn trong cơ sở dữ liệu. | Gửi request điều chỉnh với version cũ | Bị chặn lỗi xung đột phiên bản (Optimistic Locking). |
