# Tài Liệu Ngữ Cảnh Kiến Trúc Dự Án (Agent Project Context)

## 1. Tổng Quan Kiến Trúc & Cấu Trúc Thư Mục

Hệ thống Quản lý Nhân viên (Employee Management System) xây dựng theo kiến trúc **Hexagonal Architecture (Ports & Adapters) kết hợp Domain-Driven Design (DDD)** ở Backend và **Next.js App Router (React/TypeScript)** ở Frontend.

### Cấu trúc thư mục Backend (`/backend`):
```text
backend/src/main/java/com/hrm/employeemanagement/
├── domain/                      # Nghiệp vụ lõi (Pure Java, KHÔNG Spring/JPA)
│   ├── allocation/              # WeeklyProjectAllocation, WeeklyCapacityMatrixPolicy, CapacityStatus
│   ├── availability/            # WeeklyAvailability, WeeklyAvailabilityPolicy, YearWeek, Holiday
│   ├── employee/                # Employee, EmployeeId, EmployeeStatus
│   ├── project/                 # Project, ProjectId, ProjectStatus
│   ├── authorization/           # PermissionCode, DataScope
│   ├── audit/                   # AuditLog
│   └── exception/               # DomainException, AllocationCapacityExceededException...
├── application/                 # Ứng dụng & Điều phối nghiệp vụ (Use Cases)
│   ├── dto/                     # Command & Result DTOs (AllocateResourceCommand, WeeklyCapacityResult...)
│   ├── port/
│   │   ├── inbound/             # AllocateResourceUseCase, GetCompanyWeeklyCapacityUseCase...
│   │   └── outbound/            # LoadWeeklyProjectAllocationPort, SaveWeeklyProjectAllocationPort,
│   │                            # LoadWeeklyAvailabilityPort, SaveAuditLogInNewTransactionPort...
│   └── service/                 # Service thực thi Use Cases (ResourceAllocationService...)
└── infrastructure/              # Chi tiết kỹ thuật & Framework (Spring Boot, JPA, MySQL, Flyway)
    ├── adapter/
    │   ├── inbound/web/         # REST Controllers (ResourceAllocationController, GlobalExceptionHandler)
    │   └── outbound/persistence/# JPA Repositories & Adapters (WeeklyProjectAllocationRepositoryAdapter...)
    ├── config/                  # Bean wiring & Security configuration
    └── transaction/             # Decorators quản lý Transaction & Retry (RetryableAllocateResourceUseCaseDecorator...)
```

### Cấu trúc thư mục Database & Migrations:
```text
backend/src/main/resources/db/migration/
├── V1__init_user_management_schema.sql
├── ...
├── V29__create_weekly_project_allocations.sql
├── V30__create_project_resource_demands.sql
├── V35__grant_resource_allocation_permissions.sql
├── ...
└── V60__create_cascade_delay_warning_schema.sql (Migration mới nhất hiện tại)
```

---

## 2. Request Flow Chuẩn Thực Tế

```text
HTTP Request (Client / Next.js)
    ↓
ResourceAllocationController (Inbound Web Adapter - REST API, Validation @Valid)
    ↓
AllocateResourceUseCase (Inbound Port)
    ↓
[RetryableAllocateResourceUseCaseDecorator] (Transaction & Optimistic Locking Retry)
    ↓
ResourceAllocationService (Application Service - Orchestration, RBAC & DataScope check)
    ↓
Domain Policies & Entities (WeeklyCapacityMatrixPolicy, WeeklyAvailabilityPolicy, WeeklyProjectAllocation)
    ↓
Outbound Ports (LoadWeeklyAvailabilityPort, LoadWeeklyProjectAllocationPort, SaveWeeklyProjectAllocationPort, SaveAuditLogInNewTransactionPort)
    ↓
Outbound Adapters (JPA Repository Adapters, RequiresNewAuditLogRepositoryAdapter)
    ↓
Database (MySQL via Spring Data JPA)
```

---

## 3. Allocation & Capacity Planning Flow

1. **Nhận Command**: `AllocateResourceCommand` mang `employeeId`, `projectId`, `year`, `weekNumber`, `allocatedHours`.
2. **Kiểm tra RBAC & Data Scope**:
   - `authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_MANAGE)` -> Đảm bảo user có quyền quản lý phân bổ.
   - Kiểm tra `DataScope` của user với `orgUnitId` của nhân sự và `orgUnitId` của dự án (`ORGANIZATION_BRANCH` / `COMPANY`).
3. **Pessimistic Lock & Kiểm tra trạng thái**:
   - `loadEmployeePort.findByIdForUpdate(new EmployeeId(command.employeeId()))` khóa dòng employee nhằm tránh race condition khi phân bổ song song cho cùng 1 nhân sự.
   - Kiểm tra trạng thái `ACTIVE` và hạn hợp đồng (`contractEndDate >= weekStartDate`).
   - Kiểm tra dự án `ACTIVE`.
4. **Tính toán số giờ khả dụng (Net Available Hours)**:
   - Load `WeeklyAvailability` của nhân sự trong tuần (`YearWeek`).
   - `netAvailableHours = availabilityOpt.map(WeeklyAvailability::getNetAvailableHours).orElse(standardHours)`.
   - Giờ khả dụng ròng đã được khấu trừ tự động ngày lễ và nghỉ phép đã duyệt (`APPROVED`) theo QTN-10 (`WeeklyAvailabilityPolicy`).
5. **Tính toán tổng giờ đã phân bổ (Allocated Hours)**:
   - Load tất cả các dòng phân bổ hiện có của nhân sự trong tuần: `loadAllocationPort.loadAllocationsForEmployee(employeeId, yearWeek)`.
   - Loại trừ bản ghi của chính dự án đang phân bổ:
     `otherProjectsAllocatedSum = existingAllocations.stream().filter(a -> !a.getProjectId().equals(command.projectId())).map(WeeklyProjectAllocation::getAllocatedHours).reduce(BigDecimal.ZERO, BigDecimal::add)`.
   - `totalRequestedAllocated = otherProjectsAllocatedSum.add(command.allocatedHours())`.
6. **Kiểm tra vượt năng lực (Capacity Limit Check - QTN-11)**:
   - Nếu `totalRequestedAllocated > netAvailableHours`: Quá tải (`isOverloaded = true`).
   - Yêu cầu xác nhận và nhập `overloadReason` nếu user là Resource Manager (`VT-03`).
7. **Lưu dữ liệu & Kiểm toán**:
   - Cập nhật dòng phân bổ hiện tại hoặc tạo mới: `saveAllocationPort.save(allocation)`.
   - Ghi nhật ký kiểm toán qua `SaveAuditLogInNewTransactionPort` chạy `REQUIRES_NEW`.

---

## 4. RBAC, Data Scope & Security Enforcement

- **Hệ thống 6 Vai trò (Roles)**:
  - `VT-01` (Ban giám đốc): `COMPANY` - Chỉ xem (Read-only).
  - `VT-02` (Quản lý dự án - PM): `SELF` / `ORGANIZATION_BRANCH` trên dự án - Đề xuất phân bổ, không có quyền tự xác nhận quá tải.
  - `VT-03` (Quản lý nguồn lực - RM): `ORGANIZATION_BRANCH` - Toàn quyền phân bổ và phê duyệt vượt tải kèm lý do.
  - `VT-04` (Nhân viên chuyên môn), `VT-05` (Nhân sự), `VT-06` (Quản trị viên): Không có quyền quản lý phân bổ nguồn lực.
- **Enforcement Rules**:
  - Backend là chốt chặn bảo mật duy nhất, không phụ thuộc vào frontend disabled/hidden button.
  - Từ chối truy cập ném `PermissionDeniedException` -> Chuyển thành HTTP 403 bởi `GlobalExceptionHandler`.
  - Ghi security log khi cố tình xác nhận vượt tải trái phép (`ACCESS_DENIED_OVERLOAD_CONFIRM`).

---

## 5. Persistence & Database Conventions

- **RDBMS**: MySQL 8.x.
- **Khóa chính (PK)**: `BIGINT AUTO_INCREMENT` (Tuyệt đối KHÔNG dùng UUID).
- **Khóa ngoại User**: `user_id BIGINT` tham chiếu `users(id)`.
- **Kiểu thời gian**: `TIMESTAMP` hoặc `DATETIME` (Tuyệt đối KHÔNG dùng PostgreSQL `TIMESTAMPTZ`).
- **Tuần & Năm**: Bảng `weekly_project_allocations` lưu `year_number INT` và `week_number INT` (KHÔNG có cột `week_start_date`).
- **Flyway Migration Versioning**: Số migration tiếp theo là `V61__...`.
- **Audit Logging**: Bảng `audit_logs` (`id`, `user_id`, `action`, `table_name`, `record_id`, `created_at`, `old_value`, `new_value`). Chạy trên transaction độc lập `REQUIRES_NEW`.
