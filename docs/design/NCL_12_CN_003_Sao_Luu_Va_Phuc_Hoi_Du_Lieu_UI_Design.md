# Thiết kế Giao diện NCL-12-CN-003: Sao lưu và Phục hồi Dữ liệu

## 1. Mục tiêu Thiết kế
- Cung cấp giao diện quản trị hiện đại, an toàn và dễ thao tác dành riêng cho Quản trị viên hệ thống (Admin - `VT-06`).
- Đảm bảo tuân thủ Design System của hệ thống (Tailwind CSS, Lucide icons, Dark/Light themed palette, Responsive layout).
- Thiết kế trải nghiệm người dùng với các rào chắn an toàn (Guardrails): quy trình phục hồi 2 bước, trạng thái trực quan theo mã màu, cảnh báo ghi đè, và nhật ký kiểm toán trực tiếp.

---

## 2. Bố cục & Các Thành phần Giao diện

### 2.1. Thanh Tiêu đề & Công cụ Điều khiển (Header & Actions)
- **Tiêu đề chính**: `Sao lưu & Phục hồi Dữ liệu`
- **Mô tả phụ**: *Quản trị các bản sao lưu cơ sở dữ liệu, thiết lập lịch tự động và khôi phục an toàn dữ liệu kế hoạch nguồn lực.*
- **Nút tác vụ chính**:
  1. `[+] Tạo bản sao lưu ngay` (Primary action button, màu xanh dương/indigo): Mở Modal tạo bản sao lưu tức thì.
  2. `[⚙] Cấu hình lịch tự động` (Secondary button, icon Settings): Mở Modal cấu hình lịch sao lưu định kỳ.
  3. `[↑] Tải lên bản sao lưu` (Outline button, icon Upload): Cho phép tải tệp sao lưu `.sql` / `.json.gz` từ máy tính lên hệ thống.
  4. `[↻] Làm mới` (Icon button): Tải lại dữ liệu trang tức thời.

### 2.2. Thẻ Thống kê & Giám sát (KPI Metric Cards)
Hàng 4 thẻ thống kê tổng quan:
1. **Tổng số bản sao lưu**: Số lượng bản sao lưu hiện có trong hệ thống (kèm số bản thủ công & tự động).
2. **Bản sao lưu gần nhất**: Thời gian tạo bản sao lưu thành công gần nhất (ví dụ: `Hôm nay lúc 02:00:15`) kèm dung lượng.
3. **Dung lượng lưu trữ**: Tổng dung lượng file sao lưu đang sử dụng trên máy chủ (ví dụ: `45.8 MB`).
4. **Trạng thái lịch tự động**: Badge trạng thái (`Đang bật - Hàng ngày 02:00` hoặc `Đang tắt`), thời điểm sao lưu dự kiến tiếp theo.

### 2.3. Khu vực Nội dung Đa chức năng (Tabbed Content Area)
Hệ thống 2 tab chính:
- **Tab 1: Danh sách bản sao lưu (Backups Management)**:
  - Thanh tìm kiếm theo mã/tên bản sao và bộ lọc loại (`Tất cả`, `FULL`, `RESOURCE_PLAN`) và trạng thái (`COMPLETED`, `IN_PROGRESS`, `FAILED`).
  - **Bảng dữ liệu chi tiết**:
    - `Mã bản sao`: Badge hiển thị mã (ví dụ: `BCK-20260922-100520-4A9F`).
    - `Tên & Ghi chú`: Tiêu đề kèm mô tả mục đích sao lưu.
    - `Loại sao lưu`: Badge `Toàn hệ thống (FULL)` hoặc `Kế hoạch nguồn lực (PLAN)`.
    - `Thời điểm tạo`: Ngày giờ tạo chính xác và cờ `Tự động` / `Thủ công`.
    - `Dung lượng`: Kích thước file định dạng rõ ràng (MB / KB).
    - `Mã Checksum`: 8 ký tự đầu của SHA-256 kèm tooltip xem đầy đủ và nút sao chép nhanh.
    - `Trạng thái`: Badge màu (Xanh lá: `Hoàn tất`, Vàng: `Đang xử lý`, Đỏ: `Lỗi`).
    - `Hành động`:
      - Nút **Phục hồi** (Icon RotateCcw, màu cam/đỏ): Bị vô hiệu hóa nếu trạng thái không phải `COMPLETED`. Mở Modal phục hồi 2 bước.
      - Nút **Tải về** (Icon Download): Tải tệp sao lưu về máy.
      - Nút **Xóa** (Icon Trash2): Xóa bản sao lưu với xác nhận xóa.
- **Tab 2: Nhật ký thao tác & An toàn (Audit Logs Trail)**:
  - Bảng ghi nhận toàn bộ lịch sử: Thời điểm, Người thực hiện, Hành động (`Tạo mới`, `Phục hồi`, `Xóa`, `Tải về`, `Cấu hình lịch`, `Từ chối truy cập`), Trạng thái (`Thành công`, `Thất bại`, `Bị chặn`), Địa chỉ IP và Ghi chú chi tiết.

### 2.4. Chi tiết Thiết kế Các Dialog / Modal

#### Modal 1: Tạo bản sao lưu (Create Backup Modal)
- Tiêu đề: *Tạo bản sao lưu dữ liệu mới*
- Trường nhập:
  - Tên/Tiêu đề bản sao lưu (bắt buộc).
  - Phân loại sao lưu: Radio options `Toàn bộ hệ thống (FULL)` hoặc `Kế hoạch nguồn lực & Chấm công (RESOURCE_PLAN)`.
  - Mô tả & Ghi chú (tùy chọn).
- Nút bấm: `Hủy` và `Bắt đầu sao lưu`. Có hiệu ứng tiến trình đang xử lý (loading spinner).

#### Modal 2: Phục hồi dữ liệu 2 bước an toàn (Two-Step Restore Modal)
- **Bước 1: Cảnh báo rủi ro & Chi tiết bản sao lưu**:
  - Banner cảnh báo màu đỏ/cam: *"Thao tác này sẽ ghi đè toàn bộ dữ liệu hiện tại về trạng thái tại thời điểm tạo bản sao lưu. Hệ thống sẽ tự động tạo một điểm an toàn trước khi phục hồi."*
  - Bảng thông tin bản sao lưu: Mã, Thời điểm tạo, Dung lượng, Loại dữ liệu.
  - Nút: `Tiếp tục bước xác nhận`.
- **Bước 2: Xác nhận chuỗi an toàn & Lý do**:
  - Hộp nhập chuỗi: Yêu cầu người dùng gõ chính xác từ khóa `RESTORE`.
  - Hộp nhập lý do phục hồi: Bắt buộc (tối thiểu 10 ký tự, ví dụ: "Khắc phục sự cố xung đột lịch phân bổ tuần 38").
  - Nút: `Quay lại` và `Xác nhận và Phục hồi ngay` (Màu đỏ cảnh báo).

#### Modal 3: Cấu hình Lịch sao lưu tự động (Backup Schedule Modal)
- Bật/Tắt tự động sao lưu định kỳ (Toggle Switch).
- Tần suất: Hàng ngày (`DAILY`) hoặc Hàng tuần (`WEEKLY` - chọn thứ trong tuần).
- Giờ sao lưu trong ngày (Time picker, mặc định 02:00 sáng).
- Thời gian lưu trữ (Retention Period, ví dụ 30 ngày, các bản sao quá hạn sẽ tự động dọn dẹp).

---

## 3. Trạng thái Giao diện Đặc biệt
- **Loading State**: Skeleton loading cho các thẻ thống kê và hàng trong bảng.
- **Empty State**: Hiển thị minh họa kèm nút kêu gọi hành động "Tạo bản sao lưu đầu tiên".
- **403 Forbidden Access State**: Khi người dùng không phải Quản trị viên (`VT-06`), hiển thị màn hình từ chối truy cập rõ ràng và thông báo đã ghi nhận nhật ký bảo mật.
