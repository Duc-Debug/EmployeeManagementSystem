# Tài liệu Phân tích Nghiệp vụ: Phân bổ nhân sự thuê ngoài vào dự án

## 1. Tổng quan Use Case
- **Mã yêu cầu**: NCL-14-CN-002
- **Tên Use Case**: Phân bổ nhân sự thuê ngoài vào dự án (Allocating Outsourced Personnel to Projects)
- **Thuộc Epic**: NCL-14 (Năng lực thuê ngoài - Outsource Capacity)
- **Vai trò thực hiện (Actors)**:
  - **Quản lý nguồn lực (Resource Manager - VT-03)**: Chủ thể chính thực hiện tìm kiếm, điều phối và phân bổ giờ làm theo tuần cho nhân sự thuê ngoài.
  - **Quản lý dự án (Project Manager - VT-02)**: Tiếp nhận nhân sự thuê ngoài vào dự án, giao việc và theo dõi khối lượng hoàn thành.
  - **Nhân sự (HR Specialist - VT-05)**: Giám sát và quản lý hợp đồng đối tác, hồ sơ nhân sự thuê ngoài.
  - **Ban giám đốc (Executive - VT-01)**: Xem báo cáo năng lực và tỷ trọng nguồn lực thuê ngoài toàn đơn vị.
- **Mục tiêu**: Cho phép Quản lý nguồn lực (RM) phân bổ nhân sự thuê ngoài vào các dự án cụ thể theo từng tuần, đảm bảo tuyệt đối tuân thủ quy tắc ràng buộc về thời hạn hợp đồng (`QTN-21`), năng lực khả dụng tuần (`QTN-11`), cảnh báo quá tải (`QTN-12`), ghi vết kiểm toán & thông báo cho Quản lý dự án (`QTN-15`).

---

## 2. Bối cảnh & Vấn đề Thực tế
- Ở các đơn vị phát triển phần mềm và tư vấn, việc sử dụng nhân sự thuê ngoài (freelancer, chuyên gia tư vấn hoặc nhân sự của đối tác vendor) là phương án phổ biến để bổ sung kịp thời các kỹ năng chuyên sâu hoặc bù đắp năng lực thiếu hụt theo từng giai đoạn dự án.
- Khác với nhân viên chính thức, nhân sự thuê ngoài luôn có thời hạn hợp đồng cố định (`startDate` và `contractEndDate`) cùng đơn vị cung cấp cụ thể (`providerName`).
- **Các rủi ro thường gặp nếu không có kiểm soát chặt chẽ:**
  1. **Phân bổ ngoài hạn hợp đồng**: PM hoặc RM xếp lịch nhân sự outsource vào các tuần trước ngày hợp đồng có hiệu lực hoặc sau ngày hợp đồng đã chấm dứt, dẫn đến vỡ kế hoạch thực tế hoặc phát sinh chi phí phạt hợp đồng.
  2. **Không phân biệt được nguồn lực**: Khi tìm kiếm nhân tài theo kỹ năng, RM không phân biệt được đâu là nhân viên nội bộ, đâu là nhân sự thuê ngoài kèm đơn vị đối tác, gây khó khăn cho việc tối ưu chi phí dự án.
  3. **Thiếu cảnh báo trực quan trên bảng năng lực tuần**: Bảng điều phối năng lực hiển thị như nhân viên bình thường khiến người xếp lịch dễ chọn nhầm vào các tuần sau khi hợp đồng kết thúc.

---

## 3. Quy tắc Nghiệp vụ Chi tiết (Business Rules)

### BR-01: Thẩm quyền và Phạm vi Dữ liệu (RBAC & Data Scope)
- Chỉ người dùng có vai trò **Quản lý nguồn lực (VT-03)** với quyền `RESOURCE_ALLOCATION_MANAGE` mới được phép thực hiện phân bổ giờ làm cho nhân sự thuê ngoài.
- Nhân sự thuê ngoài thuộc bộ phận/phòng ban tiếp nhận nào thì chỉ RM phụ trách cây phân cấp phòng ban đó (`scope_org_unit_id`) mới có thẩm quyền phân bổ.

### BR-02: Ràng buộc Thời hạn Hợp đồng (QTN-21 Enforcement)
- Nhân sự thuê ngoài (`isOutsourced == true`) **chỉ được phép phân bổ vào các tuần nằm trong khoảng hiệu lực hợp đồng** từ `startDate` đến `contractEndDate`.
- **Quy tắc tuần làm việc (ISO YearWeek):**
  - Tuần kết thúc trước ngày bắt đầu hợp đồng: $WeekEndDate < StartDate \rightarrow$ **Bị chặn**.
  - Tuần bắt đầu sau ngày kết thúc hợp đồng: $WeekStartDate > ContractEndDate \rightarrow$ **Bị chặn**.
  - Khi bị chặn, hệ thống ném ngoại lệ nghiệp vụ chuyên biệt `OutsourcedContractPeriodException` với thông báo nêu rõ thông tin nhân sự, đơn vị cung cấp và khoảng hiệu lực hợp đồng.

### BR-03: Ràng buộc Phân bổ Hàng loạt (Bulk Allocation)
- Khi thực hiện phân bổ nhiều tuần liên tiếp (`BulkResourceAllocationService`):
  - Hệ thống tự động thẩm định từng tuần trong dải tuần được chọn.
  - Những tuần nằm ngoài khoảng `[startDate, contractEndDate]` sẽ được đưa vào danh sách `blockedWeeks` với mã nguyên nhân `CONTRACT_OUT_OF_BOUNDS`.
  - Các tuần hợp lệ bên trong hạn hợp đồng vẫn được xử lý phân bổ bình thường (phân bổ từng phần thành công).

### BR-04: Ràng buộc Điều chỉnh & Dời tuần (Move Week)
- Khi thực hiện dời tuần phân bổ (`AdjustResourceAllocationService` với action `MOVE_WEEK`):
  - Hệ thống bắt buộc kiểm tra tuần đích `targetWeek`. Nếu tuần đích nằm ngoài thời hạn hợp đồng của nhân sự thuê ngoài, thao tác bị từ chối với lỗi hợp đồng không hợp lệ.

### BR-05: Nhận diện Nguồn lực Thuê ngoài & Tìm kiếm Kỹ năng
- Trong giao diện và API tìm kiếm ứng viên theo kỹ năng (`RESOURCE_SEARCH`):
  - Trả về thông tin: `isOutsourced`, `providerName`, `startDate`, `contractEndDate`.
  - Hỗ trợ bộ lọc loại nhân sự: Tất cả / Nội bộ / Thuê ngoài.
  - Hiển thị nhãn Badge "Thuê ngoài" (Outsourced) nổi bật kèm tên đơn vị cung cấp trên giao diện.

### BR-06: Trực quan hóa trên Bảng Năng lực Tuần (Capacity Matrix)
- Trên bảng năng lực tuần của bộ phận:
  - Hiển thị nhãn "Thuê ngoài" và tên đối tác bên cạnh tên nhân sự.
  - Các cột tuần nằm ngoài thời hạn hợp đồng của nhân sự thuê ngoài được làm mờ (disabled pattern) và hiển thị tooltip "Ngoài thời hạn hợp đồng thuê ngoài", ngăn chặn việc click chuột phân bổ nhầm.

### BR-07: Lưu vết Kiểm toán & Thông báo PM (Audit Trail & Notification)
- Mọi thao tác phân bổ, điều chỉnh nhân sự thuê ngoài đều được ghi nhật ký vào `audit_logs` (action `ALLOCATE_OUTSOURCED_RESOURCE` hoặc `ADJUST_OUTSOURCED_ALLOCATION`).
- Hệ thống tự động gửi thông báo in-app cho Quản lý dự án (PM) của dự án được phân bổ nhân sự thuê ngoài (`QTN-15`).

---

## 4. Bảng Kịch bản Kiểm thử Nghiệp vụ (Acceptance Criteria)

| Mã TC | Tiền điều kiện | Thao tác | Kết quả mong đợi |
|---|---|---|---|
| **TC-01** | Nhân sự thuê ngoài có hợp đồng từ 01/06/2026 đến 31/08/2026. RM thuộc đúng phòng ban. | Phân bổ 40h vào tuần 26/2026 (cuối tháng 6) | Thành công (HTTP 200/201). Lưu phân bổ, ghi nhật ký kiểm toán và gửi thông báo cho PM dự án. |
| **TC-02** | Nhân sự thuê ngoài có hợp đồng từ 01/06/2026 đến 31/08/2026. | RM phân bổ vào tuần 15/2026 (tháng 4 - trước hạn) | Bị chặn bởi `OutsourcedContractPeriodException` với thông báo hợp đồng chưa có hiệu lực tại tuần đã chọn. |
| **TC-03** | Nhân sự thuê ngoài có hợp đồng từ 01/06/2026 đến 31/08/2026. | RM phân bổ vào tuần 40/2026 (tháng 10 - sau hạn) | Bị chặn bởi `OutsourcedContractPeriodException` với thông báo hợp đồng đã kết thúc trước tuần đã chọn. |
| **TC-04** | Nhân sự thuê ngoài hợp đồng từ tuần 23 đến tuần 30. | RM phân bổ hàng loạt từ tuần 20 đến tuần 35 | Các tuần 20-22 và 31-35 bị chặn và trả về trong `blockedWeeks` (mã `CONTRACT_OUT_OF_BOUNDS`). Các tuần 23-30 phân bổ thành công. |
| **TC-05** | Nhân sự thuê ngoài đã có phân bổ ở tuần 25. | RM thực hiện dời tuần (`MOVE_WEEK`) sang tuần 36 (ngoài hạn hợp đồng) | Bị từ chối điều chỉnh do tuần đích nằm ngoài thời hạn hợp đồng. |
| **TC-06** | Tìm kiếm nhân sự theo kỹ năng Java và mức thành thạo 3. | Người dùng mở kết quả tìm kiếm | Ứng viên thuê ngoài hiển thị nhãn "Thuê ngoài", tên đối tác cung ứng và thời hạn hợp đồng còn lại. |
| **TC-07** | Bảng năng lực tuần hiển thị nhân sự thuê ngoài. | Xem các tuần sau khi hợp đồng kết thúc | Các ô tuần bị vô hiệu hóa hiển thị gạch chéo mờ kèm tooltip cảnh báo "Ngoài thời hạn hợp đồng". |
| **TC-08** | Người dùng có vai trò VT-04 (Nhân viên) hoặc VT-06 (Admin). | Thử gọi API phân bổ nhân sự thuê ngoài | Bị từ chối quyền truy cập (HTTP 403 Forbidden). |

