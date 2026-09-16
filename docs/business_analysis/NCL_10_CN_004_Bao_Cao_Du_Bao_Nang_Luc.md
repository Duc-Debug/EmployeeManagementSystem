# Kế hoạch triển khai NCL-10-CN-004

## 1. Thông tin chung

- Tên chức năng: Báo cáo dự báo năng lực các tuần tới
- Mã User Story: `NCL-10-CN-004`
- Epic: `NCL-10` - Báo cáo năng lực và bảng điều khiển
- Vai trò chính: Ban giám đốc (`VT-01`)
- Vai trò được xem theo Acceptance Criteria: Ban giám đốc (`VT-01`) và Quản lý nguồn lực (`VT-03`)
- Độ ưu tiên: Quan trọng
- Story Point: 5
- Người phụ trách trong kế hoạch dự án: Thành viên một
- Chu kỳ: Chu kỳ số năm
- Phụ thuộc: `NCL-06-CN-002` và `NCL-06-CN-005`

## 2. Mục tiêu nghiệp vụ

Ban giám đốc cần nhìn thấy năng lực còn trống trong các tuần tương lai để quyết định thời điểm nhận thêm dự án hoặc chủ động tìm việc lấp phần năng lực chưa sử dụng.

Báo cáo phải tách rõ:

- Tổng giờ khả dụng.
- Tổng giờ đã cam kết cho các dự án.
- Tổng giờ đang giữ chỗ cho dự án dự kiến.
- Giờ còn trống chưa tính giữ chỗ.
- Giờ còn trống sau khi tính cả giữ chỗ.

Giữ chỗ không phải cam kết chính thức và không được cộng vào giờ đã cam kết.

## 3. Phạm vi

### 3.1. Trong phạm vi

- Chọn khoảng tuần tương lai theo tuần ISO.
- Lọc theo đơn vị tổ chức trong phạm vi người dùng được phép xem.
- Tổng hợp số liệu theo từng tuần.
- Hiển thị biểu đồ đường hoặc biểu đồ vùng cho năng lực còn trống.
- Hiển thị bảng số liệu chi tiết theo tuần.
- Tách riêng giờ giữ chỗ và giờ đã cam kết.
- Hiển thị trạng thái tuần: còn trống, gần đầy hoặc vượt năng lực.
- Ghi audit log khi xem báo cáo thành công và khi bị từ chối truy cập.
- Xử lý các trạng thái tải dữ liệu, không có dữ liệu và lỗi.

### 3.2. Ngoài phạm vi

- Tự động dự đoán dự án mới bằng AI hoặc mô hình thống kê.
- Tự động phân bổ nhân sự.
- Thay đổi phân bổ hoặc giữ chỗ từ màn hình báo cáo.
- Xuất báo cáo ra tệp; đây là phạm vi của `NCL-10-CN-003`.
- Lưu snapshot báo cáo định kỳ.
- So sánh nhiều kịch bản mô phỏng.

## 4. Quy tắc nghiệp vụ

### BR-01. Khoảng thời gian

- Báo cáo chỉ nhận tuần hiện tại hoặc tuần tương lai.
- Mặc định hiển thị 12 tuần, phù hợp Acceptance Criteria.
- Đề xuất cho phép chọn 4, 8, 12 hoặc 16 tuần.
- Việc chuyển năm phải tuân theo chuẩn ISO week và tái sử dụng `YearWeek` hiện có.

### BR-02. Tổng giờ khả dụng

Tổng giờ khả dụng của tuần là tổng `netAvailableHours` của các nhân sự nằm trong phạm vi báo cáo.

Giờ khả dụng ròng phải sử dụng cùng cách tính với bảng năng lực hiện tại, bao gồm:

- Giờ làm việc chuẩn.
- Lịch làm việc.
- Ngày lễ.
- Nghỉ phép đã duyệt.
- Ngày kết thúc hợp đồng nếu nằm trong tuần.

### BR-03. Tổng giờ đã cam kết

Tổng giờ đã cam kết là tổng `allocatedHours` từ các dòng phân bổ chính thức trong tuần. Không cộng các dòng giữ chỗ đang `ACTIVE`.

### BR-04. Tổng giờ giữ chỗ

Tổng giờ giữ chỗ là tổng `reservedHours` của các bản ghi `resource_reservations` có trạng thái `ACTIVE` trong tuần.

Các bản ghi `CONVERTED` hoặc `CANCELLED` không được tính vào giữ chỗ. Khi đã chuyển thành phân bổ chính thức, số giờ chỉ xuất hiện trong giờ cam kết để tránh tính hai lần.

### BR-05. Giờ còn trống

```text
committedRemainingHours = availableHours - committedHours
projectedRemainingHours = availableHours - committedHours - reservedHours
```

Không ép hai giá trị này về 0 trong dữ liệu báo cáo. Giá trị âm cần được giữ để thể hiện số giờ vượt năng lực.

### BR-06. Tỷ lệ sử dụng

```text
committedUtilization = committedHours / availableHours * 100
projectedUtilization = (committedHours + reservedHours) / availableHours * 100
```

- Nếu `availableHours > 0`, làm tròn một chữ số thập phân.
- Nếu `availableHours = 0` và tổng giờ bằng 0, tỷ lệ là `0%`.
- Nếu `availableHours = 0` và có giờ cam kết hoặc giữ chỗ, tỷ lệ là `null`; giao diện hiển thị `Không xác định` và đánh dấu vượt năng lực.

### BR-07. Phân quyền và phạm vi dữ liệu

- `VT-01`: xem toàn công ty hoặc lọc theo đơn vị.
- `VT-03`: chỉ xem đơn vị mình quản lý và các đơn vị con theo data scope.
- Vai trò khác: từ chối với HTTP `403` và ghi audit log.
- Backend là nơi quyết định quyền cuối cùng; ẩn menu ở frontend không thay thế kiểm tra backend.

### BR-08. Dữ liệu rỗng

Nếu khoảng tuần hợp lệ nhưng chưa có phân bổ hoặc giữ chỗ, báo cáo vẫn trả về các tuần và tổng giờ khả dụng. Nếu không có nhân sự trong phạm vi, trả danh sách tuần với các giá trị bằng 0 và thông báo không có dữ liệu nhân sự.

## 5. Kết quả mong đợi

Mỗi tuần trả về:

- Năm và số tuần ISO.
- Ngày bắt đầu và kết thúc tuần.
- Tổng giờ khả dụng.
- Tổng giờ đã cam kết.
- Tổng giờ giữ chỗ.
- Giờ còn trống chưa tính giữ chỗ.
- Giờ còn trống sau khi tính giữ chỗ.
- Tỷ lệ sử dụng đã cam kết.
- Tỷ lệ sử dụng dự kiến.
- Trạng thái năng lực.

Phần tổng kết trả về:

- Tổng số tuần trong báo cáo.
- Tổng giờ khả dụng.
- Tổng giờ đã cam kết.
- Tổng giờ giữ chỗ.
- Tổng giờ còn trống dự kiến.
- Số tuần vượt năng lực.
- Thời điểm tạo báo cáo.

## 6. Đánh giá dự án hiện tại

### 6.1. Thành phần có thể tái sử dụng

- `YearWeek` cho xử lý tuần ISO.
- `GetCompanyWeeklyCapacityService` cho cách tính năng lực ròng, lịch làm việc, ngày lễ, nghỉ phép, hợp đồng và data scope.
- `WeeklyCapacityMatrixPolicy` cho các trường hợp `availableHours = 0` và tỷ lệ sử dụng.
- `LoadWeeklyProjectAllocationPort` để đọc phân bổ chính thức theo nhân sự và khoảng tuần.
- `LoadResourceReservationPort` để đọc giữ chỗ đang hoạt động.
- `AuthorizationService`, `PermissionCode` và `SaveAuditLogPort` cho phân quyền và nhật ký.
- `RecruitmentDemandReportService` và `RecruitmentDemandReportController` làm mẫu tổ chức module báo cáo.
- `CompanyWeeklyCapacityView` và API `getCompanyWeeklyCapacityMatrix` làm mẫu hiển thị dữ liệu năng lực trên frontend.

### 6.2. Khoảng trống hiện tại

- Chưa có use case và API riêng cho báo cáo dự báo năng lực.
- Chưa có permission riêng cho báo cáo này.
- Chưa có DTO tổng hợp năng lực theo tuần ở cấp công ty/bộ phận dành riêng cho báo cáo.
- Chưa có trang frontend và mục điều hướng riêng.
- Chưa có kiểm thử cho bốn Acceptance Criteria của `NCL-10-CN-004`.

### 6.3. Quyết định về database

Không cần tạo bảng dữ liệu báo cáo mới vì toàn bộ số liệu có thể tổng hợp từ dữ liệu hiện có:

- `employees` và dữ liệu giờ khả dụng.
- `weekly_project_allocations`.
- `resource_reservations`.
- Lịch làm việc, ngày lễ và nghỉ phép đã duyệt.
- `audit_logs`.

Cần một Flyway migration mới chỉ để khai báo permission `CAPACITY_FORECAST_REPORT_READ` và gán cho `VT-01`, `VT-03`, nếu permission này chưa tồn tại. Migration đã được nhóm thống nhất dành cho chức năng này là `V89__add_capacity_forecast_report_permission.sql`. Không dùng `V83` vì version đó thuộc phần việc của thành viên khác và không sửa các migration cũ.

## 7. Thiết kế backend dự kiến

### 7.1. Cấu trúc package

```text
application/
  dto/report/capacityforecast/
    CapacityForecastQuery.java
    CapacityForecastResult.java
  port/inbound/report/capacityforecast/
    GetCapacityForecastUseCase.java
  service/report/capacityforecast/
    GetCapacityForecastService.java

infrastructure/
  adapter/inbound/web/report/capacityforecast/
    CapacityForecastReportController.java
    CapacityForecastReportExceptionHandler.java
```

Chỉ tạo outbound port hoặc persistence adapter mới nếu các port hiện tại không hỗ trợ truy vấn theo khoảng tuần với hiệu năng phù hợp.

### 7.2. API đề xuất

```http
GET /api/v1/reports/capacity-forecast
```

Query parameters:

```text
orgUnitId    optional
fromYear     required
fromWeek     required
durationWeeks optional, default 12, allowed 4..16
```

Response rút gọn:

```json
{
  "orgUnitId": 10,
  "orgUnitName": "Delivery",
  "fromYear": 2026,
  "fromWeek": 38,
  "durationWeeks": 12,
  "weeks": [
    {
      "year": 2026,
      "weekNumber": 38,
      "startDate": "2026-09-14",
      "endDate": "2026-09-20",
      "availableHours": 400,
      "committedHours": 300,
      "reservedHours": 40,
      "committedRemainingHours": 100,
      "projectedRemainingHours": 60,
      "committedUtilization": 75.0,
      "projectedUtilization": 85.0,
      "status": "AVAILABLE"
    }
  ],
  "summary": {
    "totalAvailableHours": 4800,
    "totalCommittedHours": 3600,
    "totalReservedHours": 480,
    "totalProjectedRemainingHours": 720,
    "overCapacityWeeks": 0
  },
  "generatedAt": "2026-09-15T09:00:00"
}
```

### 7.3. Luồng xử lý

1. Validate `fromYear`, `fromWeek` và `durationWeeks`.
2. Yêu cầu permission `CAPACITY_FORECAST_REPORT_READ`.
3. Xác định `orgUnitId` hiệu lực theo vai trò và data scope.
4. Tạo danh sách tuần ISO liên tiếp.
5. Tải nhân sự trong phạm vi.
6. Tải hàng loạt giờ khả dụng, nghỉ phép, ngày lễ và lịch làm việc.
7. Tải hàng loạt phân bổ chính thức.
8. Tải hàng loạt giữ chỗ `ACTIVE`.
9. Tổng hợp số liệu theo tuần và áp dụng các công thức nghiệp vụ.
10. Ghi audit log cho lần xem báo cáo thành công.
11. Trả kết quả theo thứ tự tuần tăng dần.

Không truy vấn từng nhân sự trong vòng lặp. Các dữ liệu phải được tải theo danh sách nhân sự và danh sách tuần để tránh N+1 query.

## 8. Thiết kế frontend dự kiến

### 8.1. Vị trí

- Component: `frontend/src/components/reports/CapacityForecastReportView.tsx`
- API client: `frontend/src/lib/api/capacity-forecast.ts`
- Test: `frontend/src/tests/capacity-forecast-report.test.mjs`
- Điều hướng: thêm mục `Dự báo năng lực` trong nhóm báo cáo hoặc năng lực.

### 8.2. Bố cục màn hình

1. Tiêu đề và mô tả ngắn.
2. Bộ lọc đơn vị, tuần bắt đầu và số tuần.
3. Các thẻ tổng quan: khả dụng, cam kết, giữ chỗ, còn trống dự kiến.
4. Biểu đồ theo tuần với ba chuỗi:
   - Giờ khả dụng.
   - Giờ đã cam kết.
   - Giờ còn trống dự kiến.
5. Bảng chi tiết theo tuần.
6. Chú giải nêu rõ giữ chỗ chưa phải cam kết.
7. Empty state, error state và loading state.

### 8.3. Quy tắc hiển thị

- Giờ giữ chỗ dùng màu khác với giờ cam kết.
- Giờ còn trống âm hiển thị màu cảnh báo và giữ dấu âm.
- Tooltip biểu đồ phải hiện đủ các số liệu của tuần.
- Không cho chỉnh sửa phân bổ hoặc giữ chỗ trên màn hình báo cáo.
- Frontend chỉ hiển thị mục điều hướng cho `VT-01`, `VT-03` nhưng vẫn xử lý HTTP `403` từ backend.

## 9. Kế hoạch kiểm thử

### 9.1. Domain và công thức

- Tính đúng giờ còn trống khi không có giữ chỗ.
- Tách đúng giờ giữ chỗ khỏi giờ cam kết.
- Giữ giá trị âm khi vượt năng lực.
- Xử lý `availableHours = 0` an toàn.
- Không tính giữ chỗ đã `CONVERTED` hoặc `CANCELLED`.
- Không tính trùng dòng giữ chỗ đã chuyển thành phân bổ.
- Chuyển tuần qua năm đúng chuẩn ISO.

### 9.2. Application service

- TC-01: dữ liệu 12 tuần trả đúng giờ khả dụng, cam kết và còn trống từng tuần.
- TC-02: giữ chỗ được trả riêng và ảnh hưởng đúng tới giờ còn trống dự kiến.
- TC-03: `VT-01`, `VT-03` truy cập được; vai trò khác nhận từ chối và có audit log.
- TC-04: xem thành công ghi người dùng, bộ phận, khoảng tuần và thời điểm.
- `VT-03` không xem được dữ liệu ngoài data scope.
- Không có nhân sự hoặc không có phân bổ vẫn trả response hợp lệ.
- Input tuần hoặc độ dài khoảng không hợp lệ bị từ chối bằng lỗi có mã rõ ràng.

### 9.3. Persistence và API

- Truy vấn phân bổ và giữ chỗ theo nhiều tuần không bị N+1.
- API trả đúng HTTP `200`, `400`, `403`.
- Response giữ đúng thứ tự tuần.
- Kiểm tra migration permission và role mapping trên MySQL/H2 theo cách dự án đang dùng.

### 9.4. Frontend

- Render đúng 12 tuần mặc định.
- Thay đổi bộ lọc gọi API với query đúng.
- Biểu đồ và bảng dùng cùng một nguồn số liệu.
- Empty, loading, error và forbidden state hiển thị đúng.
- Phân biệt trực quan giờ cam kết và giờ giữ chỗ.
- Giá trị âm và tuần vượt năng lực có cảnh báo rõ ràng.
- Menu chỉ hiện cho `VT-01`, `VT-03`.

## 10. Thứ tự triển khai

### Bước 1. Chốt nghiệp vụ

- Chốt khoảng tuần mặc định và giới hạn tối đa.
- Chốt cách hiển thị khi giờ còn trống âm.
- Chốt phạm vi của `VT-03` theo cây tổ chức.
- Chốt permission riêng và tên mã permission.

### Bước 2. Backend nền

- Thêm permission bằng migration mới.
- Tạo Query, Result, inbound use case và application service.
- Tái sử dụng các outbound port hiện có; chỉ mở rộng khi thiếu truy vấn hàng loạt.
- Thêm controller, mapping response và exception handling.

### Bước 3. Kiểm thử backend

- Viết domain/application test trước cho công thức và phân quyền.
- Viết persistence/API integration test.
- Chạy toàn bộ test backend để phát hiện hồi quy.

### Bước 4. Frontend

- Tạo API types và client.
- Tạo màn hình báo cáo, bộ lọc, KPI, biểu đồ và bảng.
- Gắn vào Dashboard và Sidebar theo phân quyền.
- Viết frontend test.

### Bước 5. Kiểm tra hoàn tất

- Đối chiếu bốn Acceptance Criteria.
- So sánh tổng giờ báo cáo với bảng năng lực hiện có trên cùng phạm vi và khoảng tuần.
- Xác nhận giờ giữ chỗ không bị cộng vào cam kết.
- Xác nhận audit log và data scope.
- Chạy build, test và lint của backend/frontend.

## 11. Tiêu chí hoàn thành

- Báo cáo mặc định hiển thị chính xác 12 tuần tương lai.
- Mỗi tuần có đủ giờ khả dụng, cam kết, giữ chỗ và còn trống.
- Giữ chỗ được tách khỏi cam kết và không tính hai lần.
- Kết quả khớp bảng năng lực theo cùng phạm vi dữ liệu.
- `VT-01`, `VT-03` truy cập đúng; vai trò khác bị từ chối.
- Data scope được kiểm tra tại backend.
- Lượt xem thành công và truy cập bị từ chối đều có audit log.
- Không phát sinh N+1 query.
- Backend test, frontend test, lint và build đều đạt.

## 12. Điểm cần người phụ trách xác nhận trước khi bắt đầu

1. `VT-03` có được xem toàn công ty hay chỉ nhánh tổ chức mình quản lý? Đề xuất: chỉ trong data scope.
2. Báo cáo mặc định bắt đầu từ tuần hiện tại hay tuần kế tiếp? Đề xuất: tuần hiện tại.
3. Khoảng tối đa là 12 hay 16 tuần? Đề xuất: mặc định 12, tối đa 16.
4. Giờ còn trống có được âm hay ép về 0? Đề xuất: giữ số âm để biểu diễn phần vượt năng lực.
5. Có cần xuất tệp ngay trong UC này không? Đề xuất: không, để `NCL-10-CN-003` xử lý.
6. Có cần lưu snapshot báo cáo không? Đề xuất: không; báo cáo tính theo dữ liệu hiện thời và ghi audit log lượt xem.
