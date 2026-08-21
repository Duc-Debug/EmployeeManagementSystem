# 🎨 HƯỚNG DẪN PHÁT TRIỂN FRONTEND

## Employee Management System

Tài liệu này quy định các nguyên tắc kiến trúc, tổ chức mã nguồn, cách xây dựng UI, quản lý trạng thái, giao tiếp API và tiêu chuẩn phát triển Frontend của dự án Employee Management System.

Mục tiêu:

- Mã nguồn dễ đọc và dễ bảo trì.
- Các thành viên sử dụng cùng một cách tổ chức code.
- Component có trách nhiệm rõ ràng.
- Tách biệt UI, business logic và giao tiếp API.
- Hạn chế code trùng lặp.
- TypeScript được sử dụng đúng mục đích.
- Dễ kiểm thử và mở rộng.
- Giao diện thống nhất giữa các chức năng.

---

# 1. Công nghệ sử dụng

Frontend sử dụng các công nghệ chính:

- React
- TypeScript
- Vite
- React Router
- ESLint

Các thư viện bổ sung chỉ được sử dụng khi có lý do kỹ thuật rõ ràng.

Không thêm thư viện chỉ vì một chức năng nhỏ nếu có thể giải quyết bằng công cụ hoặc abstraction hiện có của dự án.

---

# 2. Nguyên tắc kiến trúc

Frontend được tổ chức theo nguyên tắc phân tách trách nhiệm.

```text
┌─────────────────────────────────────────────┐
│                   UI                        │
│                                             │
│ Pages / Components                          │
│                                             │
├─────────────────────────────────────────────┤
│              APPLICATION                    │
│                                             │
│ Hooks / UI Logic / State                    │
│                                             │
├─────────────────────────────────────────────┤
│               DATA ACCESS                   │
│                                             │
│ API Client / Services                       │
│                                             │
├─────────────────────────────────────────────┤
│                 TYPES                       │
│                                             │
│ Domain Types / API Types                    │
└─────────────────────────────────────────────┘
```
Nguyên tắc:
```
Page
 ↓
Component
 ↓
Hook / Application Logic
 ↓
API Service
 ↓
Backend
```
Không để Component gọi HTTP trực tiếp nếu việc gọi API có thể được tách thành API Service.

# 3. Cấu trúc thư mục

Cấu trúc ban đầu:
```
src/
├── assets/
├── components/
├── features/
├── hooks/
├── layouts/
├── pages/
├── services/
├── types/
├── utils/
├── routes/
├── App.tsx
└── main.tsx
```
Khi hệ thống phát triển, ưu tiên tổ chức theo Feature/Domain thay vì tạo một thư mục khổng lồ chứa tất cả Component.

Ví dụ:
```
src/
├── features/
│   ├── employee/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── services/
│   │   ├── types/
│   │   └── pages/
│   │
│   ├── department/
│   └── ...
│
├── components/
│   ├── common/
│   └── layout/
│
├── layouts/
├── routes/
├── services/
├── types/
└── utils/
```
Không bắt buộc mọi Feature phải có toàn bộ thư mục con.

Chỉ tạo thư mục khi thực sự có nội dung cần đặt vào đó.

# 4. Component
## 4.1. Single Responsibility

Một Component nên có một trách nhiệm chính.

Không tạo Component vừa:

- Hiển thị UI.
- Gọi nhiều API.
- Xử lý business logic.
- Quản lý nhiều loại state.
- Chuyển đổi dữ liệu phức tạp.

Ví dụ không nên:
```typescript
function EmployeePage() {
  // gọi API


  // xử lý validation


  // xử lý permission


  // xử lý pagination


  // xử lý business logic


  // render toàn bộ UI
}
```
Nên phân tách:
```
EmployeePage
    ↓
EmployeeList
    ↓
EmployeeTable
    ↓
EmployeeRow
```
và:
```
EmployeePage
    ↓
useEmployees()
    ↓
employeeService
    ↓
API
```
# 5. Page

Page chịu trách nhiệm điều phối giao diện của một màn hình.

Page có thể:

- Lấy dữ liệu thông qua Hook.
- Kết hợp các Component.
- Xử lý trạng thái loading/error ở cấp màn hình.
- Điều hướng.

Page không nên chứa toàn bộ UI của màn hình trong một file lớn.

Ví dụ:
```
EmployeePage
├── EmployeeHeader
├── EmployeeFilter
├── EmployeeTable
├── EmployeePagination
└── EmployeeDialog
```
# 6. Component dùng chung

Các Component dùng chung phải được đặt ở khu vực phù hợp.

Ví dụ:
```
components/
├── common/
│   ├── Button
│   ├── Input
│   ├── Modal
│   ├── Table
│   └── Pagination
│
└── layout/
    ├── Header
    ├── Sidebar
    └── Footer
```
Không đưa Component đặc thù của Employee vào components/common.

Ví dụ:
```
❌ components/common/EmployeeSalaryTable
```
Nếu chỉ dùng cho Employee:
```
✅ features/employee/components/EmployeeSalaryTable
```
# 7. Hooks

Custom Hook được sử dụng để đóng gói logic có thể tái sử dụng hoặc logic của một Feature.

Ví dụ:
```
useEmployees()
useEmployee()
useCreateEmployee()
useAuth()
usePagination()
```
Hook không nên trở thành nơi chứa toàn bộ ứng dụng.

Không tạo một Hook khổng lồ xử lý mọi thứ:
```
❌ useEmployeeManagementEverything()
```
Mỗi Hook nên có trách nhiệm rõ ràng.

# 8. API Service

Giao tiếp với Backend phải được tập trung qua API Service.

Ví dụ:
```
features/
└── employee/
    └── services/
        └── employeeService.ts
```
Ví dụ:
```typescript
export const employeeService = {
  getEmployees,
  getEmployeeById,
  createEmployee,
  updateEmployee,
  deleteEmployee,
};
```
Component không nên chứa:
```typescript
fetch("/api/employees");
```
trực tiếp nếu API Service đã được định nghĩa.

Luồng chuẩn:
```
Component
    ↓
Hook
    ↓
Service
    ↓
HTTP Client
    ↓
Backend API
```
# 9. TypeScript

Không sử dụng any một cách tùy tiện.

Không nên:
```typescript
const employee: any = response.data;
```
Phải định nghĩa Type rõ ràng:
```typescript
interface Employee {
  id: string;
  employeeCode: string;
  fullName: string;
  email: string;
}
```
## 9.1. Type cho API

Request và Response nên có type riêng khi cần.

Ví dụ:
```
CreateEmployeeRequest
UpdateEmployeeRequest
EmployeeResponse
EmployeeListResponse
```
Không dùng một Type duy nhất cho mọi trường hợp nếu cấu trúc dữ liệu khác nhau.

# 10. Naming Convention
### Component

PascalCase:
```
EmployeeTable
EmployeeForm
EmployeeDetail
File Component
```
Ưu tiên PascalCase:
```
EmployeeTable.tsx
EmployeeForm.tsx
```
### Hook

camelCase và bắt đầu bằng use:
```
useEmployees.ts
useEmployeeForm.ts
```
### Service

camelCase:
```
employeeService.ts
authService.ts
```
### Type

PascalCase:
```
Employee
CreateEmployeeRequest
EmployeeResponse
```
### Function

camelCase:
```
createEmployee()
getEmployeeById()
updateEmployee()
```
### Constant

UPPER_SNAKE_CASE:
```
DEFAULT_PAGE_SIZE
MAX_FILE_SIZE
```
# 11. State Management

Không đưa mọi dữ liệu vào Global State.

Phân biệt:
```
Local State
Global State
Server State
URL State
Form State
Local State
```
Dùng cho trạng thái chỉ thuộc một Component:
```
isModalOpen
selectedTab
inputValue
Server State
```
Dữ liệu lấy từ Backend nên được quản lý theo cơ chế phù hợp với Server State.

Không sao chép dữ liệu API vào nhiều Global State khác nhau nếu không cần thiết.

### Global State

Chỉ đưa dữ liệu thực sự cần chia sẻ rộng rãi vào Global State.

Ví dụ:
```
Authentication
Current User
Global UI State
```
Không đưa:
```
EmployeeTableData
DepartmentList
EmployeeFormValue
```
vào Global State chỉ vì nhiều Component cùng nằm trong một Page.

# 12. Form

Form phải có cấu trúc rõ ràng.

Tách:
```
Form UI
Form State
Validation
Submit Handler
API Call
```
Validation phải nhất quán với Business Rule và API Contract.

Frontend Validation giúp cải thiện trải nghiệm người dùng.

Tuy nhiên:

- Frontend Validation không thay thế Backend Validation.

- Backend vẫn phải kiểm tra dữ liệu và Business Rule.

# 13. Error Handling

Frontend phải xử lý tối thiểu các trạng thái:
```
Loading
Success
Empty
Error
```
Ví dụ:
```
Loading
   ↓
Success → Data
   │
   └── Empty → Empty State


Error → Error State
```
Không để lỗi API bị bỏ qua.

Không hiển thị trực tiếp thông tin kỹ thuật nhạy cảm cho người dùng.

Ví dụ không hiển thị:
```
SQL Exception
Stack Trace
Internal Server Error Details
```
Thay vào đó sử dụng thông báo phù hợp với người dùng.

# 14. Authentication và Authorization

Frontend chịu trách nhiệm:
```
Hiển thị trạng thái đăng nhập.
Quản lý UI theo quyền.
Điều hướng người dùng.
Bảo vệ trải nghiệm người dùng.
```
Ví dụ:
```
ADMIN
   → Hiển thị màn hình quản lý nhân viên


EMPLOYEE
   → Không hiển thị chức năng quản trị
```
Tuy nhiên:

- Frontend không phải nơi quyết định quyền truy cập cuối cùng.

- Backend phải luôn kiểm tra Authentication và Authorization.

- Không được coi việc ẩn Button là cơ chế bảo mật.
```
     Ẩn UI
        ≠
    Bảo mật API
```
# 15. Routing

Route phải được tổ chức tập trung và có cấu trúc rõ ràng.

Ví dụ:
```
routes/
├── AppRoutes.tsx
├── ProtectedRoute.tsx
└── PublicRoute.tsx
```
Phân biệt:
```
Public Routes
Protected Routes
Role-based Routes
```
Không rải logic kiểm tra quyền ở nhiều Component khác nhau nếu có thể tập trung hóa.

# 16. UI và Styling

UI phải thống nhất về:
```
Typography.
Spacing.
Button.
Input.
Form.
Table.
Modal.
Notification.
Loading.
Error State.
Empty State.
```
Không tự tạo nhiều phiên bản của cùng một UI Component nếu Component dùng chung đã tồn tại.

Ví dụ:
```
❌ EmployeeButton
❌ DepartmentButton
❌ UserButton
```
nếu chúng chỉ khác nhau về tên.

Nên sử dụng:
```
Button
```
với các props phù hợp.

# 17. Responsive Design

Giao diện phải được thiết kế phù hợp với kích thước màn hình mà hệ thống hỗ trợ.

Không sử dụng kích thước cố định một cách tùy tiện.

Đặc biệt chú ý:
```
Table.
Form.
Sidebar.
Modal.
Navigation.
Dashboard.
```
Các màn hình quan trọng phải được kiểm tra ở các kích thước phù hợp trước khi Merge.

# 18. Performance

Không tối ưu sớm khi chưa có vấn đề thực tế.

Tuy nhiên phải tránh:
```
Render không cần thiết.
Gọi API lặp lại.
Request khi không cần thiết.
Load toàn bộ dữ liệu khi chỉ cần pagination.
Component quá lớn.
Asset quá nặng.
```
Khi danh sách lớn, ưu tiên:
```
Pagination
Filtering
Sorting
Search
```
ở phía Backend khi phù hợp.

# 19. Accessibility

UI phải ưu tiên khả năng sử dụng cho người dùng.

Các Component tương tác phải có:
```
Label rõ ràng.
Trạng thái Focus.
Keyboard interaction phù hợp.
Alt text cho hình ảnh cần thiết.
Thông báo lỗi dễ hiểu.
Semantic HTML khi phù hợp.
```
Không sử dụng ```<div> ```thay thế mọi phần tử HTML chỉ vì thuận tiện.

# 20. Testing

Các logic quan trọng phải được kiểm thử.

Ưu tiên kiểm tra:
```
Component
    ↓
Hook
    ↓
Form
    ↓
User Interaction
    ↓
Integration
```
Test các trường hợp:
```
Hiển thị đúng dữ liệu.
Loading.
Empty.
Error.
Validation.
User interaction.
Permission.
Navigation.
```
Không chỉ kiểm tra Happy Path.

# 21. Không để Business Logic nằm trong UI

UI chỉ nên chịu trách nhiệm hiển thị và tương tác.

Không nên:
```typescript
function EmployeeForm() {


  if (
    employee.status === "ACTIVE" &&
    employee.department === "HR" &&
    ...
  ) {
    // business rule phức tạp
  }
}
```
Nếu đây là Business Rule, phải xác định nguồn chính thức của rule và xử lý phù hợp.

Frontend có thể thực hiện các kiểm tra phục vụ UX, nhưng Backend vẫn là nơi bảo vệ Business Rule cuối cùng.

# 22. Không gọi API trực tiếp từ nhiều nơi

Không nên:
```
EmployeePage → fetch(...)
EmployeeTable → fetch(...)
EmployeeDialog → fetch(...)
EmployeeDetail → fetch(...)
```
mỗi nơi tự xây dựng URL, headers và xử lý response khác nhau.

Nên:
```
Employee Feature
       ↓
Employee Service
       ↓
HTTP Client
       ↓
Backend
```
Điều này giúp:
```
Dễ thay đổi API.
Dễ xử lý Authentication.
Dễ xử lý Error.
Dễ Test.
Giảm code trùng lặp.
```
# 23. Environment Configuration

Không hard-code URL Backend:
```typescript
const API_URL = "http://localhost:8080";
```
Sử dụng Environment Variable phù hợp với Vite.

Ví dụ:
```
VITE_API_BASE_URL
```
Không commit:
```
.env
.env.local
```
nếu chứa Secret hoặc thông tin môi trường riêng.

Có thể commit:
```
.env.example
```
để mô tả các biến cần thiết.

# 24. Không lưu Secret ở Frontend

Mọi biến được build vào Frontend đều có khả năng được người dùng xem.

Do đó không lưu:
```
Database Password
JWT Secret
Private API Key
Server Secret
```
trong Frontend Environment Variables.

Frontend chỉ được sử dụng các thông tin thực sự cần thiết ở phía Client.

# 25. Code Quality

Ưu tiên:
```
Code dễ đọc.
Component nhỏ.
Logic rõ ràng.
Type rõ ràng.
Tên biến có ý nghĩa.
Không lặp code.
Không abstraction quá mức.
Không sử dụng any tùy tiện.
Không để dead code.
Không để console log phục vụ debug trong production.
```
Không tạo abstraction chỉ để làm code có vẻ "kiến trúc" hơn.

# 26. Git và Component Changes

Một Pull Request nên tập trung vào một mục đích rõ ràng.

Ví dụ:
```
Feature:
Create employee
```
Không nên đồng thời:
```
Create employee
+
Redesign sidebar
+
Refactor authentication
+
Change dashboard
```
trong cùng một PR nếu không có lý do cần thiết.

# 27. Code Review Checklist

Reviewer cần kiểm tra tối thiểu:

### Kiến trúc
- Component có trách nhiệm rõ ràng.
- Business Logic không bị nhúng vào UI.
- API Call được tách khỏi Component.
- Không có dependency không cần thiết.
### TypeScript
- Không sử dụng any tùy tiện.
- Request/Response có Type phù hợp.
- Không dùng type sai mục đích.
### State
- Local State được dùng đúng phạm vi.
- Không đưa dữ liệu không cần thiết vào Global State.
- Server State được xử lý phù hợp.
### API
- API Service được sử dụng đúng.
- Loading được xử lý.
- Error được xử lý.
- Empty State được xử lý khi cần.
### UI
- UI thống nhất với hệ thống.
- Responsive phù hợp.
- Form có validation.
- Accessibility cơ bản được đảm bảo.
### Security
- Không có Secret trong Frontend.
- Không coi UI hiding là cơ chế bảo mật.
- Authorization phía Backend vẫn được đảm bảo.
### Testing
- Logic quan trọng có test.
- Happy Path được kiểm tra.
- Error/Edge Case được kiểm tra.
# 28. Nguyên tắc cuối cùng

Frontend chịu trách nhiệm về trải nghiệm người dùng, không phải là nơi bảo vệ Business Rule cuối cùng.

Frontend phải:
```
Dễ sử dụng
Dễ hiểu
Phản hồi tốt
Có validation
Có xử lý lỗi
```
Backend phải:
```
Xác thực
Phân quyền
Kiểm tra dữ liệu
Bảo vệ Business Rule
Bảo vệ dữ liệu
```
Hai phía phải thống nhất thông qua API Contract:
```
Frontend
    ↓
API Contract
    ↓
Backend
```
Mọi thay đổi lớn về:
```
UI Architecture
State Management
API Contract
Authentication
Authorization
Routing
Shared Components
```
phải được review trước khi triển khai.