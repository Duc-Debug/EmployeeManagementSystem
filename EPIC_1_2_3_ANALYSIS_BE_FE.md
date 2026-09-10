# BÁO CÁO PHÂN TÍCH HIỆN TRẠNG BE & FE: EPIC 1, EPIC 2, EPIC 3
> **Tài liệu căn cứ**: `docs/plans/De_tai_2_Ke_Hoach_Nguon_Luc.xlsx` (Product Backlog, Epics, Business Rules)  
> **Dự án**: Hệ thống Quản lý Kế hoạch Nguồn lực Doanh nghiệp (Employee Management System)  
> **Ngày phân tích**: 10/09/2026  
> **Kiến trúc đối chiếu**: Backend Spring Boot (Hexagonal Architecture / DDD), Frontend React TypeScript (Vite/Tailwind)

---

## 📑 BẢNG TỔNG HỢP TIẾN ĐỘ 3 EPIC (21 USER STORIES)

| STT | Mã Story | Tên Chức Năng | Backend (BE) | Frontend (FE) | Tình trạng Data FE | Đánh giá hoàn thành |
| :---: | :--- | :--- | :---: | :---: | :---: | :---: |
| **EPIC 1** | **NCL-01** | **ĐĂNG NHẬP VÀ PHÂN QUYỀN THEO CÂY TỔ CHỨC** | | | | **100%** |
| 1 | NCL-01-CN-001 | Đăng nhập hệ thống (JWT, Rate limit, Audit) | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 2 | NCL-01-CN-002 | Quản lý tài khoản người dùng | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 3 | NCL-01-CN-003 | Khai báo cây tổ chức (Org Units tree) | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 4 | NCL-01-CN-004 | Phân quyền theo vai trò & phạm vi (Data Scope) | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 5 | NCL-01-CN-005 | Đổi mật khẩu & Khôi phục mật khẩu | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 6 | NCL-01-CN-006 | Khóa và mở lại tài khoản | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| **EPIC 2** | **NCL-02** | **HỒ SƠ NHÂN SỰ VÀ NĂNG LỰC** | | | | **85%** |
| 7 | NCL-02-CN-001 | Quản lý hồ sơ nhân sự & giờ làm việc chuẩn | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 8 | NCL-02-CN-002 | Khai báo hồ sơ kỹ năng nhân sự (VT-04) | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 9 | NCL-02-CN-003 | Khai báo giờ khả dụng theo tuần (QTN-10/16) | ✅ Đầy đủ | ⚠️ Tách biệt | Real API có, UI form thiếu | **80%** (Có API, FE tích hợp chưa mượt vào view) |
| 10 | NCL-02-CN-004 | Tìm nhân sự theo kỹ năng & mức độ rảnh | ✅ Đầy đủ | ⚠️ Chưa nối | ❌ Dùng Mock Data | **60%** (BE có `/allocations/search`, FE đang mock) |
| 11 | NCL-02-CN-005 | Quản lý danh mục kỹ năng (Skill Catalog) | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 12 | NCL-02-CN-006 | Xác nhận mức thành thạo của nhân sự (VT-03) | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 13 | NCL-02-CN-007 | Ma trận kỹ năng của bộ phận | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| **EPIC 3** | **NCL-03** | **DỰ ÁN VÀ CÂY CÔNG VIỆC** | | | | **80%** |
| 14 | NCL-03-CN-001 | Tạo dự án | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 15 | NCL-03-CN-002 | Chia hạng mục & công việc (Cây WBS) | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 16 | NCL-03-CN-003 | Đặt ngân sách giờ công cho công việc (QTN-06) | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 17 | NCL-03-CN-004 | Đóng dự án (QTN-08: Close & Reopen) | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |
| 18 | NCL-03-CN-005 | Tạo dự án từ mẫu (Template Seed V39) | ✅ Đầy đủ | ⚠️ Một phần | Cần nối modal FE | **85%** (BE xong API & seed, FE cần nối UI chọn mẫu) |
| 19 | NCL-03-CN-006 | Quản lý mốc tiến độ của dự án (Milestones) | ✅ Đầy đủ | ⚠️ Tách rời | ❌ Dùng Mock Data | **70%** (BE có CRUD Milestone, FE ở components/task chưa gắn) |
| 20 | NCL-03-CN-007 | Ước lượng nhu cầu nhân sự theo vai trò | ✅ Đầy đủ | ⚠️ Tách rời | ❌ Chưa có UI chuẩn | **60%** (BE có controller demand, FE chưa dựng UI) |
| 21 | NCL-03-CN-008 | Nhân bản cây công việc từ dự án cũ (Clone WBS) | ✅ Đầy đủ | ✅ Đã có | Real Data (API) | **100%** |

---

## 🔎 PHÂN TÍCH CHI TIẾT TỪNG EPIC

---

### 🏛️ EPIC 1: NCL-01 - ĐĂNG NHẬP VÀ PHÂN QUYỀN THEO CÂY TỔ CHỨC
*Mục tiêu*: Kiểm soát truy cập và giới hạn phạm vi dữ liệu theo vai trò và nhánh tổ chức (QTN-01).

#### 1. NCL-01-CN-001: Đăng nhập hệ thống
- **Backend (BE)**:
  - `AuthController.java`: `POST /api/v1/auth/login`
  - Cơ chế: JWT Bearer Token, Spring Security 6, `LoginRateLimiter` (chống brute force), lưu log `AuditLogPort`.
- **Frontend (FE)**:
  - `auth-session.ts`, `lib/api/auth.ts`, `LoginPage.tsx`. Lưu token, tự động gắn Header Authorization `Bearer ...`.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 2. NCL-01-CN-002: Quản lý tài khoản người dùng
- **Backend (BE)**:
  - `UserController.java`: CRUD `POST /api/v1/users`, `GET /api/v1/users`, `PUT /api/v1/users/{id}`
  - Validation: Tên đăng nhập duy nhất, mật khẩu chuẩn bảo mật, gắn `orgUnitId` và `roleId`.
- **Frontend (FE)**:
  - `pages/EmployeeProfilePage.tsx`, `components/employee/form/EmployeeDetailModal.tsx`.
  - Kết nối API `lib/api/users.ts` để hiển thị danh sách, phân trang, lọc bộ phận.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 3. NCL-01-CN-003: Khai báo cây tổ chức
- **Backend (BE)**:
  - `OrgUnitController.java`: `POST /api/v1/org-units`, `GET /api/v1/org-units/tree`, `PUT /api/v1/org-units/{id}`, `PUT /api/v1/org-units/{id}/move`, `PATCH /api/v1/org-units/{id}/status`.
  - Cấu trúc cha-con (Tree), chống loop cha-con, kiểm tra nhân sự trước khi ngừng hoạt động.
- **Frontend (FE)**:
  - `components/department/DepartmentsView.tsx`, `components/ui/OrgUnitCombobox.tsx`.
  - Hiển thị cây phòng ban trực quan, kéo thả / chọn cấp quản lý theo nhánh.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 4. NCL-01-CN-004: Phân quyền theo vai trò và phạm vi dữ liệu (Data Scope)
- **Backend (BE)**:
  - `RoleController.java`: `GET /api/v1/roles`
  - `UserController.java`: `PUT /api/v1/users/{id}/role`
  - Quy tắc `QTN-01`: Hệ thống kiểm tra `dataScope` (`ALL`, `ORG_BRANCH`, `SELF`) trong các Service thông qua `UserDataScopeService` và annotation `@PreAuthorize`.
- **Frontend (FE)**:
  - `components/access/AccessControlView.tsx`: Cấu hình vai trò, gán quyền và gán phạm vi dữ liệu (Toàn công ty, Nhánh phòng ban, Cá nhân).
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 5. NCL-01-CN-005: Đổi mật khẩu và khôi phục mật khẩu
- **Backend (BE)**:
  - `AuthController.java`: `POST /api/v1/auth/change-password`, `POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password`.
  - Có giới hạn tần suất gửi yêu cầu `ForgotPasswordRateLimiter`, token reset có hạn dùng 15 phút, outbox worker gửi email giả lập/thật.
- **Frontend (FE)**:
  - `ChangePasswordModal.tsx`: Form đổi mật khẩu cá nhân khi đã đăng nhập.
  - `ForgotPasswordModal.tsx`: Modal 2 bước kính mờ Aero (Gửi mã qua Email & Đặt lại mật khẩu) kết nối API thật `/api/v1/auth/forgot-password` và `/api/v1/auth/reset-password`.
  - `ResetPasswordPage.tsx`: Trang đặt lại mật khẩu độc lập hỗ trợ tự động điền Token từ link gửi về email (`/reset-password?token=...`).
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 6. NCL-01-CN-006: Khóa và mở lại tài khoản
- **Backend (BE)**:
  - `UserController.java`: `PATCH /api/v1/users/{id}/status`
  - Khi khóa: Thu hồi phiên làm việc tức thì qua `UserStatusCache`, giữ nguyên lịch sử phân bổ và chấm công cũ.
- **Frontend (FE)**:
  - Nút Khóa / Kích hoạt nhanh trong `EmployeeProfilePage.tsx` có badge trạng thái và modal xác nhận.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

---

### 👥 EPIC 2: NCL-02 - HỒ SƠ NHÂN SỰ VÀ NĂNG LỰC
*Mục tiêu*: Quản lý năng lực kỹ năng, mức độ thành thạo và giờ làm việc chuẩn theo tuần để làm căn cứ phân bổ nhân sự (QTN-10, QTN-16).

#### 1. NCL-02-CN-001: Quản lý hồ sơ nhân sự và giờ làm việc chuẩn
- **Backend (BE)**:
  - `EmployeeController.java`: `POST /api/v1/employees`, `GET /api/v1/employees/{id}`, `PUT /api/v1/employees/{id}`
  - Quản lý mã nhân viên, chức danh, ngày vào làm, hợp đồng và giờ chuẩn tuần (mặc định 40h).
- **Frontend (FE)**:
  - `components/hrprofile/HrProfilePage.tsx`, `HrProfileForm.tsx`: Hiển thị danh sách, phân quyền `VT-05` được sửa toàn bộ, nhân viên thường chỉ xem bản thân.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 2. NCL-02-CN-002: Khai báo hồ sơ kỹ năng nhân sự
- **Backend (BE)**:
  - `EmployeeSkillController.java`: `POST /api/v1/employees/me/skills`
  - Kiểm tra vai trò `VT-04`, kỹ năng tạo ra ở trạng thái chờ duyệt `PENDING`.
- **Frontend (FE)**:
  - `components/skilldeclaration/SkilldeclarationView.tsx` (Tab *Khai báo cá nhân*).
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 3. NCL-02-CN-003: Khai báo giờ khả dụng theo tuần
- **Backend (BE)**:
  - `WeeklyAvailabilityController.java`: 
    - `PUT /api/v1/employees/{employeeId}/availability` (khai báo số giờ chuẩn tuần).
    - `GET /api/v1/employees/{employeeId}/capacity` (tính giờ khả dụng = Giờ chuẩn - Lễ - Nghỉ phép theo `QTN-10`).
- **Frontend (FE)**:
  - Hiện tại FE đang hiển thị con số giờ chuẩn mặc định (40h) trong `HrProfileCard.tsx`. Form cho phép cập nhật lịch làm việc theo tuần chi tiết đang cần một popup hoặc tab riêng trong hồ sơ nhân viên.
- **Trạng thái**: ⚠️ **Hoàn thành 80% (BE đã có đủ API, FE cần gắn form chi tiết)**.

#### 4. NCL-02-CN-004: Tìm nhân sự theo kỹ năng và mức độ rảnh
- **Backend (BE)**:
  - `ResourceAllocationController.java`: `GET /api/v1/allocations/search`
  - Tham số tìm kiếm: `skillId`, `minProficiencyLevel`, `orgUnitId`, `fromYear`, `fromWeek`, `toYear`, `toWeek`.
  - Thuật toán BE tính toán trực tiếp công suất khả dụng còn trống của từng ứng viên theo từng tuần.
- **Frontend (FE)**:
  - `components/skilldeclaration/SkillresourceSearch.tsx` và `components/task/ProjectResourceSearch.tsx`.
  - **Vấn đề**: Hiện đang dùng **MOCK DATA** (`DEFAULT_RESOURCE_EMPLOYEES`) thay vì gọi API `GET /api/v1/allocations/search`.
- **Trạng thái**: ❌ **Hoàn thành 60% (Cần nối API search backend vào FE)**.

#### 5. NCL-02-CN-005: Quản lý danh mục kỹ năng (Skill Catalog)
- **Backend (BE)**:
  - `SkillController.java`: CRUD nhóm kỹ năng và kỹ năng: `POST /api/v1/skills`, `PUT /api/v1/skills/{id}`, `POST /api/v1/skills/merge`, `PATCH /api/v1/skills/{id}/deactivate`.
- **Frontend (FE)**:
  - `components/skilldeclaration/SkillCatalogView.tsx`: Quản lý danh mục, nhóm kỹ năng, kích hoạt/ngừng kích hoạt.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 6. NCL-02-CN-006: Xác nhận mức thành thạo của nhân sự
- **Backend (BE)**:
  - `EmployeeSkillApprovalController.java`: `GET /api/v1/employee-skills/pending`, `PUT /api/v1/employee-skills/{id}/approve`.
  - Phân quyền: Dành cho Quản lý nguồn lực (`VT-03`). Cho phép duyệt giữ nguyên hoặc điều chỉnh mức level từ 1-5 kèm ghi chú.
- **Frontend (FE)**:
  - `components/skilldeclaration/SkillApproveTable.tsx`: Danh sách chờ duyệt, nút duyệt nhanh hoặc mở modal chỉnh sửa level.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 7. NCL-02-CN-007: Ma trận kỹ năng của bộ phận
- **Backend (BE)**:
  - `DepartmentSkillMatrixController.java`: `GET /api/v1/skills/matrix?orgUnitId={id}`
  - Tính toán ma trận 2 chiều (Hàng là nhân sự, cột là kỹ năng, giá trị là mức thành thạo đã xác nhận, highlight kỹ năng chỉ có 1 người biết).
- **Frontend (FE)**:
  - `components/skilldeclaration/SkillMatrixView.tsx`: Hiển thị bảng ma trận theo phòng ban, lọc và thống kê điểm nghẽn kỹ năng.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

---

### 📊 EPIC 3: NCL-03 - DỰ ÁN VÀ CÂY CÔNG VIỆC
*Mục tiêu*: Chia dự án thành cây hạng mục & công việc (WBS), kiểm soát ngân sách giờ công, quản lý mốc tiến độ và ước lượng nhu cầu nguồn lực (QTN-04, QTN-06, QTN-08).

#### 1. NCL-03-CN-001: Tạo dự án
- **Backend (BE)**:
  - `ProjectController.java`: `POST /api/v1/projects` (CreateProjectCommand).
  - Validation: Tên dự án, đơn vị, quản lý (`managerId`), ngày bắt đầu/kết thúc, tổng giờ dự kiến.
- **Frontend (FE)**:
  - `components/project/ProjectCreateModal.tsx`: Đã kết nối API thật, load danh sách phòng ban từ `getOrgTree()`.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 2. NCL-03-CN-002: Chia hạng mục và công việc của dự án (WBS)
- **Backend (BE)**:
  - `TaskController.java`: 
    - `POST /api/v1/projects/{projectId}/tasks` (Tạo Category hoặc Task).
    - `PATCH /api/v1/projects/{projectId}/tasks/{taskId}` (Sửa công việc).
    - `GET /api/v1/projects/{projectId}/wbs` (Trả về cây phân cấp nhiều tầng).
- **Frontend (FE)**:
  - `components/project/ProjectView.tsx` & `ProjectWbsView.tsx`:
    - Đã có hàm `mapBackendWbsToUiCategories()` chuyển đổi trực tiếp `TaskNodeResult[]` từ BE sang UI.
    - Modal thêm công việc `ProjectTaskModal.tsx` gọi API `createTask()`.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 3. NCL-03-CN-003: Đặt ngân sách giờ công cho công việc
- **Backend (BE)**:
  - `TaskController.java`: `PATCH /api/v1/projects/{projectId}/tasks/{taskId}/budget`
  - Quy tắc `QTN-06`: So sánh giờ thực tế với ngân sách giờ công, tự động đánh dấu cờ `burnStatus` (`SAFE`, `WARNING`, `OVER_BUDGET`) và cảnh báo vượt 80%.
- **Frontend (FE)**:
  - `components/project/ProjectBudgetModal.tsx`: Nhập số giờ ngân sách.
  - Hiển thị badge cảnh báo quá hạn mức trên thẻ công việc và KPI Card *Cảnh báo rủi ro*.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 4. NCL-03-CN-004: Đóng và Mở lại dự án
- **Backend (BE)**:
  - `ProjectController.java`: `POST /api/v1/projects/{id}/close`, `POST /api/v1/projects/{id}/reopen`
  - Quy tắc `QTN-08`: Khi đóng dự án, hệ thống chặn thêm công việc và phân bổ mới, lưu lý do đóng `closed_reason`.
- **Frontend (FE)**:
  - Đã tích hợp action trên danh sách dự án.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

#### 5. NCL-03-CN-005: Tạo dự án từ mẫu (Template)
- **Backend (BE)**:
  - Migration: `V33` và `V39` seed 3 template chuẩn (`TPL-DEV-001`, `TPL-OPS-002`, `TPL-ERP-003`).
  - Endpoints: 
    - `GET /api/v1/projects/templates`: Danh sách mẫu + thống kê giờ.
    - `GET /api/v1/projects/templates/{id}`: Chi tiết cây WBS của mẫu.
    - `POST /api/v1/projects/from-template`: Nhân bản cây công việc mẫu sang dự án thật.
- **Frontend (FE)**:
  - Trong `ProjectCreateModal.tsx` đã có giao diện cơ bản nhưng chưa kết nối API `getProjectTemplates()` để hiển thị danh sách template cho người dùng chọn và preview.
- **Trạng thái**: ⚠️ **Hoàn thành 85% (BE 100%, FE cần nối API template)**.

#### 6. NCL-03-CN-006: Quản lý mốc tiến độ của dự án (Milestones)
- **Backend (BE)**:
  - `MilestoneController.java`: CRUD `POST`, `GET`, `PUT`, `DELETE /api/v1/projects/{projectId}/milestones`.
  - Tự động đánh giá trạng thái `ON_TRACK` hoặc `DELAYED` dựa trên ngày kế hoạch và tiến độ task liên kết.
- **Frontend (FE)**:
  - Đã có sẵn bộ component rất đẹp trong `components/task/milestone/` (`MilestoneListView.tsx`, `MilestoneFormModal.tsx`, `MilestoneStatsSection.tsx`).
  - **Vấn đề**: Các component này đang nằm riêng trong `components/task/` và chưa được gắn vào tab xem của `components/project/ProjectView.tsx`.
- **Trạng thái**: ⚠️ **Hoàn thành 70% (BE 100%, FE cần nhúng component vào màn hình dự án chính)**.

#### 7. NCL-03-CN-007: Ước lượng nhu cầu nhân sự theo vai trò (Resource Demands)
- **Backend (BE)**:
  - `ProjectResourceDemandController.java`:
    - `POST /api/v1/projects/{projectId}/resource-demands`
    - `GET /api/v1/projects/{projectId}/resource-demands`
    - Tính tổng giờ nhu cầu theo vai trò (`hoursPerWeek`) và so sánh với tổng giờ dự kiến của dự án.
- **Frontend (FE)**:
  - **Chưa có**: Chưa có bảng hoặc tab hiển thị và nhập nhu cầu nhân sự theo vai trò trong `ProjectView.tsx`.
- **Trạng thái**: ⚠️ **Hoàn thành 60% (BE đã có API, FE cần xây dựng giao diện khai báo)**.

#### 8. NCL-03-CN-008: Nhân bản cây công việc từ dự án cũ (Clone WBS)
- **Backend (BE)**:
  - `TaskController.java`: `POST /api/v1/projects/{projectId}/wbs/clone`
  - `CloneProjectWbsService.java`: Nhân bản cây hạng mục, công việc, ngân sách giờ nhưng không sao chép người phụ trách và giờ thực tế (đảm bảo đúng yêu cầu AC).
- **Frontend (FE)**:
  - `components/project/CloneWbsModal.tsx`: Cho phép chọn dự án nguồn, xem preview cây WBS, nhấn nút nhân bản và tự động reload dữ liệu.
- **Trạng thái**: ✅ **Hoàn thành 100% (Real Data)**.

---

## 🎯 DANH SÁCH 5 CÔNG VIỆC TRỌNG TÂM CẦN LÀM TIẾP CHO EPIC 1, 2, 3

1. **[NCL-03-CN-005] Hoàn thiện UI Tạo dự án từ mẫu**:
   - Thêm tab hoặc option "Tạo từ mẫu" trong `ProjectCreateModal.tsx`.
   - Gọi API `GET /api/v1/projects/templates` để hiển thị 3 mẫu có sẵn, khi chọn thì gọi `POST /api/v1/projects/from-template`.
2. **[NCL-03-CN-006] Nhúng Mốc tiến độ (Milestones) vào ProjectView**:
   - Tích hợp `MilestoneListView` (đã có sẵn trong `components/task/milestone/`) vào một tab con của `ProjectView.tsx` để người dùng quản lý mốc tiến độ trực tiếp trên dự án đang chọn.
3. **[NCL-03-CN-007] Dựng giao diện Ước lượng nhu cầu nhân sự**:
   - Thêm modal/bảng "Nhu cầu nhân sự" trong `ProjectView.tsx` gọi 2 API của `ProjectResourceDemandController`.
4. **[NCL-02-CN-004] Thay Mock Data bằng API Search nhân sự**:
   - Kết nối component `SkillresourceSearch.tsx` với API `GET /api/v1/allocations/search` để tìm kiếm nhân sự theo kỹ năng và độ rảnh thực tế từ database.
5. **Dọn dẹp thư mục `components/task/`**:
   - Chuyển các component còn giá trị sang `components/project/` để thống nhất mã nguồn, tránh phân mảnh 2 thư mục song song.
