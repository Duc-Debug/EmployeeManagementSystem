# TÀI LIỆU PHÂN TÍCH NGHIỆP VỤ (BUSINESS ANALYSIS)
## Tính năng: Đề xuất nhân sự thay thế (NCL-07-CN-002)

---

### 1. NGUYÊN TẮC CỐT LÕI & MỤC TIÊU NGHIỆP VỤ

- **Mã tính năng**: NCL-07-CN-002
- **Tên tính năng**: Đề xuất nhân sự thay thế (Suggest Replacement Personnel)
- **Vai trò sử dụng**: Quản lý nguồn lực (Resource Manager - `VT-03`), Quản trị hệ thống (Admin - `VT-06`).
- **Mục tiêu**: Khi phát hiện xung đột lịch phân bổ hoặc nghỉ phép của nhân sự (từ NCL-07-CN-001), hệ thống tự động tìm kiếm và gợi ý danh sách nhân sự thay thế đáp ứng về mặt kỹ năng (skill proficiency) và khả dụng giờ rảnh (net available hours) trong tuần xảy ra xung đột.
- **Giá trị mang lại**: Rút ngắn thời gian xử lý xung đột nguồn lực từ vài giờ (dò tay thủ công) xuống vài phút (gợi ý tự động và thao tác xác nhận nhanh).

---

### 2. ĐIỀU KIỆN TIỀN ĐỀ VÀ KẾT QUẢ ĐẦU RA

- **Điều kiện tiền đề (Pre-conditions)**:
  1. Đã phát hiện ít nhất một cảnh báo xung đột lịch phân bổ (`ScheduleConflict`) trong hệ thống.
  2. Nhân sự trong công ty đã khai báo kỹ năng (trình độ chuyên môn/mức độ thành thạo) được duyệt.
  3. Người dùng đăng nhập có vai trò Quản lý nguồn lực (có quyền `RESOURCE_SCHEDULE_CONFLICT_NOTIFY` hoặc `RESOURCE_ALLOCATION_MANAGE`).

- **Kết quả đầu ra (Post-conditions)**:
  1. Danh sách nhân sự thay thế phù hợp hiển thị kèm mức độ thành thạo kỹ năng và số giờ rảnh còn lại trong tuần.
  2. Khi xác nhận thay thế, hệ thống gửi thông báo mô phỏng (Email/System notification) cho Quản lý dự án / Nhân sự.
  3. Ghi lại nhật ký kiểm toán (Audit Log) theo dõi người thực hiện, nội dung thay thế và thời điểm thực hiện.

---

### 3. QUY TẮC NGHIỆP VỤ & THUẬT TOÁN GỢI Ý

#### 3.1. Thuật toán tìm kiếm & sàng lọc nhân sự thay thế (Candidate Matching Algorithm)
Khi mở đề xuất nhân sự thay thế cho 1 xung đột lịch (của nhân sự $E_{conflict}$ trong tuần $W$):
1. **Xác định kỹ năng yêu cầu ($Skill_{req}$)**: Lấy các kỹ năng chính/duyệt của $E_{conflict}$ có mức thành thạo cao nhất hoặc kỹ năng liên quan đến dự án đang xung đột.
2. **Sàng lọc ứng viên theo kỹ năng**:
   - Loại trừ chính nhân sự bị xung đột ($E_{candidate} \neq E_{conflict}$).
   - Chỉ chọn nhân sự có trạng thái `ACTIVE`.
   - Nhân sự phải có kỹ năng $Skill_{req}$ với mức thành thạo $Level_{candidate} \ge Level_{req}$.
3. **Tính toán số giờ rảnh khả dụng trong tuần ($NetAvailableHours$)**:
   $$\text{NetAvailableHours} = \text{StandardHours} - \text{ApprovedLeaveHours} - \text{AllocatedHours}$$
   - Chỉ giữ lại các ứng viên có $\text{NetAvailableHours} > 0$.
4. **Sắp xếp thứ tự ưu tiên (Ranking)**:
   - Ưu tiên 1: Số giờ rảnh còn lại trong tuần ($\text{NetAvailableHours}$) giảm dần.
   - Ưu tiên 2: Mức độ thành thạo kỹ năng ($Level_{candidate}$) giảm dần.
   - Ưu tiên 3: Số năm kinh nghiệm ($YearsOfExperience$) giảm dần.

---

### 4. KỊCH BẢN NGUYÊN TẮC KIỂM THỬ (TEST CASES)

#### 4.1. NCL-07-CN-002-TC-01: Luồng thành công (Success Flow)
- **Mô tả**: Phát hiện xung đột phân bổ ở 1 nhân sự và có 2 nhân sự khác cùng kỹ năng đang có giờ rảnh trong tuần đó.
- **Thao tác**: Người quản lý nguồn lực bấm "Gợi ý thay thế".
- **Kết quả**: Hệ thống hiển thị danh sách 2 nhân sự gợi ý kèm tên, mã NV, phòng ban, mức thành thạo kỹ năng và số giờ rảnh cụ thể trong tuần.

#### 4.2. NCL-07-CN-002-TC-02: Dữ liệu rỗng (Empty Data / Rare Skill)
- **Mô tả**: Không có nhân sự nào cùng kỹ năng còn rảnh trong tuần xảy ra xung đột (kỹ năng hiếm hoặc tuần cao điểm).
- **Thao tác**: Bấm "Gợi ý thay thế".
- **Kết quả**: Hệ thống hiển thị thông báo: *"Không tìm thấy người thay thế phù hợp trong tuần này do không có nhân sự cùng kỹ năng còn đủ giờ rảnh."* kèm gợi ý giải pháp: *"Gợi ý: Dời lịch phân bổ hoặc điều chỉnh khối lượng công việc."*

#### 4.3. NCL-07-CN-002-TC-03: Kiểm soát quyền truy cập (Permission Denied)
- **Mô tả**: Người dùng không thuộc vai trò Quản lý nguồn lực (ví dụ Nhân viên thông thường) cố gắng gọi API/chức năng đề xuất thay thế.
- **Thao tác**: Gửi yêu cầu mở chức năng đề xuất nhân sự thay thế.
- **Kết quả**: Hệ thống từ chối truy cập (403 Forbidden), hiển thị thông báo lỗi bảo mật và tự động ghi nhật ký từ chối (`PERMISSION_DENIED`) trong bảng `audit_logs`.

#### 4.4. NCL-07-CN-002-TC-04: Ghi nhận lịch sử & gửi thông báo (Audit Log & Notification)
- **Mô tả**: Quản lý nguồn lực chọn 1 ứng viên thay thế và bấm "Xác nhận thay thế".
- **Thao tác**: Xác nhận thao tác trên giao diện.
- **Kết quả**:
  - Hệ thống tự động gửi thông báo mô phỏng (Simulated Notification) tới PM và nhân sự liên quan.
  - Ghi nhật ký kiểm toán trong `audit_logs` với action `CONFIRM_REPLACEMENT_SUGGESTION`, lưu rõ người thực hiện, thời điểm và nội dung thay thế.
  - Cập nhật trạng thái hiển thị trên giao diện.

---

### 5. ĐẶC TẢ API BẮT BUỘC (API SPECIFICATION)

1. **`GET /api/v1/schedule-conflicts/{id}/replacement-suggestions`**
   - Lấy danh sách nhân sự gợi ý thay thế cho xung đột `{id}`.
   - Response status: `200 OK` (hoặc `403 Forbidden` nếu không có quyền).

2. **`POST /api/v1/schedule-conflicts/{id}/confirm-replacement`**
   - Xác nhận chọn nhân sự thay thế, gửi thông báo và ghi log.
   - Body: `{ "replacementEmployeeId": 12, "notes": "Thay thế cho dự án ABC" }`
   - Response status: `200 OK`
