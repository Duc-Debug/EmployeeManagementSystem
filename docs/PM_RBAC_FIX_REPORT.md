# Đối chiếu sửa lỗi PM (VT-02)

Ngày: 22/09/2026. Nguồn đối chiếu: `PM_RBAC_BUG_CHECKLIST.md` người dùng cung cấp, `docs/ROLE_BASED_ACCESS_CONTROL_GUIDE.md`, quyền và quy tắc thực tế trong backend. Không sửa tài liệu gốc trong Downloads.

## Ma trận sidebar

- [x] PM thấy đúng 15 mục: `overview`, `capacity-dashboard`, `project`, `capacity`, `schedule-conflict`, `my-schedule`, `attendance`, `availability`, `unavailability`, `leave`, `working-calendar`, `project-allocation-report`, `departments`, `skills`, `roles`.
- [x] Ẩn 9 mục còn lại trong ma trận, bao gồm mô phỏng kịch bản, hợp đồng thuê ngoài, các báo cáo ngoài quyền PM và quản trị tài khoản.
- [x] Phòng ban và vai trò chuyên môn giữ chế độ đọc; danh mục kỹ năng tuân theo quyền hiện có.
- [x] Tên dài trên sidebar xuống dòng, không bị cắt bằng dấu ba chấm.

## Bảy lỗi nghiệp vụ

| Mục | Kết quả trong mã nguồn |
| --- | --- |
| BUG-PM-01 | Hiện lịch nghỉ team cho PM, cấp `DEPARTMENT_LEAVE_READ` bằng migration V121. Backend truy vấn thành viên thuộc các dự án PM quản lý, gồm thành viên khác phòng ban; không mở lịch nghỉ toàn công ty. |
| BUG-PM-02 | Khóa nút, modal và handler tạo/giao việc khi dự án `CLOSED`; tooltip giải thích lý do. Giữ kiểm tra trạng thái dự án tại backend. |
| BUG-PM-03 | PM chỉ thao tác giữ chỗ; không mở phân bổ hàng loạt, điều chỉnh giờ, mẫu phân bổ hoặc quản lý/khóa kỳ. Ma trận phân bổ trong dự án cũng chỉ đọc đối với PM. |
| BUG-PM-04 | API trả ngân sách, giờ đã duyệt và tổng giờ đang chờ duyệt của task. Badge màu hổ phách xuất hiện từ ngưỡng 80% trước khi duyệt; cập nhật lại sau duyệt/từ chối. Thay thông báo `alert()` bằng thông báo trong trang. |
| BUG-PM-05 | PM mở mặc định tab duyệt giờ công. Tab ghi giờ chỉ hiện khi có `WORK_LOG_READ`; PM mặc định không có quyền này. Bỏ giao diện chấm công vào/ra hành chính. |
| BUG-PM-06 | Chỉ cho PM nhân bản vào dự án mình quản lý, kiểm tra ở cả frontend và backend. So sánh `managerId` với employee ID được tra từ tài khoản, không dùng nhầm user ID. |
| BUG-PM-07 | Thêm bộ lọc/nhãn thông báo phân bổ ở chuông. Thông báo mới được lưu vào notification center cùng giao dịch với bản ghi cũ; V122 bổ sung các thông báo cũ còn thiếu, giữ trạng thái đọc và thời điểm phát hành. Click mở đúng dự án, kể cả dự án không nằm trong 50 kết quả đầu. |

**Luồng nhân bản thực tế:** modal chọn dự án nguồn; dự án đích là dự án đang mở. Vì vậy kiểm tra quyền trên đích hiện tại và chỉ hiển thị nguồn PM được phép truy cập. Không tạo thêm dropdown đích không có trong ứng dụng.

**Thông báo phân bổ:** chính sách hiện tại xếp `ALLOCATION_CHANGED` là thông báo quan trọng và phát hành ngay, không gộp vào bản tin ngày/tuần. Cấu hình bật/tắt kênh của người nhận vẫn được áp dụng.

## Các mục giao diện bổ sung

- [x] Tổng quan PM: bỏ `QTN-13` và chữ `PM Shortcuts` khỏi nội dung người dùng nhìn thấy.
- [x] Bảng điều khiển năng lực: bỏ tiêu đề tiếng Anh, mã user story và dòng mô tả thừa; chuyển khoảng thời gian thành combobox; thay lựa chọn là tải dữ liệu ngay; hiển thị đúng phạm vi ban đầu; thu gọn các card KPI.
- [x] Tạo dự án: danh sách người quản lý chỉ gồm tài khoản có vai trò PM; API cung cấp mã vai trò hệ thống riêng với vai trò chuyên môn.
- [x] Quản lý dự án: thanh công cụ và các tab xuống dòng theo chiều rộng màn hình; bộ lọc bảng theo dõi dùng lưới cân đối.
- [x] Tạo/giao việc: chỉ lấy thành viên dự án đang hoạt động làm lựa chọn. Backend từ chối người chưa tham gia dự án và không tự động thêm người đó vào dự án khi giao việc.
- [x] Ước lượng nhu cầu: ẩn thao tác áp mẫu vai trò khỏi PM.
- [x] Kanban: thu gọn cột và card; click hoặc dùng bàn phím mở chi tiết công việc.
- [x] Đóng dự án: bỏ chữ `Backend` trong cảnh báo chưa hoàn thành công việc.
- [x] Năng lực tuần: ẩn bộ lọc phòng ban khi phạm vi là `SELF`; bỏ mã NCL/QTN trong nhãn, tooltip, giữ chỗ, kế hoạch kỳ và chú giải. Giữ nút lịch sử thông báo phân bổ vì PM có quyền đọc thông báo của mình.
- [x] Mô phỏng kịch bản: giữ ẩn với PM trên sidebar và bảng năng lực.
- [x] Lịch phân bổ của tôi: giữ cho PM có hồ sơ nhân viên liên kết; bỏ khối giải thích QTN-24 được nêu trong checklist.
- [x] Nghỉ phép, khai báo thời gian không sẵn sàng và giờ khả dụng: giữ vì PM có quyền và có luồng nghiệp vụ thực tế. Không ẩn các chức năng hợp lệ này.
- [x] Phân bổ theo dự án: bỏ mã `NCL-10-CN-006` và chữ `CSV` trên nút xuất báo cáo.

## Kiểm tra

- Backend: toàn bộ 2.067 test trong 320 test suite đạt, không lỗi hoặc bỏ qua; gồm kiểm tra phạm vi lịch nghỉ PM, từ chối giao việc ngoài thành viên, ngân sách trước duyệt và cầu nối thông báo phân bổ.
- Frontend: toàn bộ 217 test đạt, không lỗi hoặc bỏ qua; bổ sung kiểm tra ma trận 15 mục, ngưỡng ngân sách, khóa task của dự án đóng và card Kanban gọn.
- `npm run build`: TypeScript và Vite build thành công. Vẫn có cảnh báo kích thước bundle lớn hơn 500 kB.
- Flyway đã chạy thành công qua V122 trên cơ sở dữ liệu H2 của bộ test. Chưa chạy migration trên cơ sở dữ liệu đang sử dụng của người dùng.
- `git diff --check`: không có lỗi khoảng trắng.
- Chưa kiểm tra trực quan bằng trình duyệt: phiên làm việc không có browser kết nối. Các đánh dấu ở trên xác nhận thay đổi/đối chiếu mã nguồn, không thay thế nghiệm thu bố cục trên trình duyệt thực tế.

## Áp dụng

Khởi động lại backend với mã mới để Flyway áp dụng V121 và V122 vào cơ sở dữ liệu đích, triển khai bản frontend mới, rồi đăng xuất/đăng nhập lại tài khoản PM để cập nhật quyền trong phiên frontend.

## Bổ sung: lỗi 500 ở lịch phân bổ tuần trên database đang dùng

Kiểm tra trực tiếp MySQL local ngày 22/09/2026 đã tái hiện lỗi `1054 / 42S22: Unknown column 'feedback_note' in 'field list'`. Bảng `employee_schedule_confirmation` thiếu `feedback_note`, `feedback_at`, `confirmation_status`; `confirmed_at` vẫn bắt buộc có giá trị.

Lịch sử Flyway ghi nhận V114 từ file cũ `V114__create_employee_schedule_confirmation.sql`, còn V114 hiện tại chứa các lệnh bổ sung phản hồi. Việc tự gọi `flyway.repair()` mỗi lần khởi động đã chấp nhận checksum/nội dung mới mà không thực thi các lệnh SQL bổ sung. Bộ test trên database mới không tái hiện được trạng thái lịch sử này.

- Thêm Java migration **V123** để bổ sung phần schema còn thiếu và hai quyền `MY_ALLOCATION_READ`, `MY_ALLOCATION_CONFIRM`. Migration kiểm tra các cột đã tồn tại để tương thích cả database cũ và database mới, giữ nguyên dữ liệu hiện có.
- Bỏ tự động `repair()` khi khởi động; Flyway tiếp tục kiểm tra lịch sử và chạy migration, báo lỗi khi phát hiện migration đã áp dụng bị thay đổi.
- Đã áp dụng V123 thành công vào `employee_management_db` local. Truy vấn xác nhận từng lỗi đã chạy thành công; truy vấn phân bổ cũng thành công. Số bản ghi xác nhận và phân bổ giữ nguyên.
- **35 test liên quan đạt**, gồm hai test hồi quy cho schema cũ thiếu cột, schema đã cập nhật, bảo toàn phản hồi/xác nhận và khả năng chạy lại migration an toàn.

Lỗi thiếu cột trên database local đã được xử lý trực tiếp; tải lại màn hình lịch phân bổ tuần để nạp dữ liệu. Chưa xác minh giao diện bằng một phiên trình duyệt đăng nhập.
