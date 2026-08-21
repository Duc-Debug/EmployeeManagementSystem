# 🛡️ HƯỚNG DẪN PHÁT TRIỂN BACKEND

## Employee Management System

Tài liệu này quy định các nguyên tắc kiến trúc, thiết kế, tổ chức mã nguồn và tiêu chuẩn phát triển bắt buộc đối với Backend của dự án Employee Management System.

Mục tiêu của tài liệu là đảm bảo mã nguồn:

- Dễ đọc và dễ hiểu.
- Dễ bảo trì.
- Dễ kiểm thử.
- Dễ mở rộng.
- Có sự thống nhất giữa các thành viên.
- Bảo vệ các quy tắc nghiệp vụ khỏi bị phá vỡ.
- Hạn chế sự phụ thuộc trực tiếp vào Framework.
- Đảm bảo ranh giới rõ ràng giữa nghiệp vụ và hạ tầng kỹ thuật.

---

# 1. Công nghệ sử dụng

Backend sử dụng các công nghệ chính:

- Java 21
- Spring Boot
- Maven
- Spring Data JPA
- Hibernate
- MySQL
- Flyway
- Spring Security
- JWT

Các thư viện hoặc công nghệ bổ sung chỉ được thêm vào khi có lý do kỹ thuật rõ ràng và không làm phá vỡ kiến trúc của hệ thống.

---

# 2. Kiến trúc hệ thống

Backend sử dụng kiến trúc:

> **Hexagonal Architecture kết hợp Domain-Driven Design (DDD)**

Hệ thống được chia thành 3 tầng chính:

```text
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE                           │
│                                                             │
│ REST Controller                                             │
│ JPA / Database                                              │
│ Spring Security                                             │
│ Configuration                                               │
│ External Services                                           │
│ Mappers / Adapters                                          │
│                                                             │
│        ┌─────────────────────────────────────────┐          │
│        │             APPLICATION                 │          │
│        │                                         │          │
│        │ Use Cases                               │          │
│        │ Application Services                    │          │
│        │ Input Ports                             │          │
│        │ Output Ports                            │          │
│        │ Commands / Queries / Results            │          │
│        │                                         │          │
│        │       ┌───────────────────────┐         │          │
│        │       │        DOMAIN         │         │          │
│        │       │                       │         │          │
│        │       │ Entities              │         │          │
│        │       │ Value Objects         │         │          │
│        │       │ Domain Services       │         │          │
│        │       │ Business Rules        │         │          │
│        │       │ Domain Exceptions     │         │          │
│        │       └───────────────────────┘         │          │
│        └─────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────┘
```
Nguyên tắc phụ thuộc:
```text
Infrastructure
       ↓
Application
       ↓
Domain
```
Không được để:
```text
Domain → Application
Domain → Infrastructure
Application → Infrastructure
```
# 3. Domain Layer
## 3.1. Mục đích

Domain Layer chứa các khái niệm và quy tắc nghiệp vụ cốt lõi của hệ thống.

Đây là tầng quan trọng nhất của hệ thống.

Domain không được biết hệ thống đang sử dụng:

- Spring Boot
- Spring Data JPA
- REST
- Jackson
- MySQL
- HTTP
- JWT
## 3.2. Nguyên tắc phụ thuộc

Domain phải ưu tiên sử dụng Java thuần.

Không được sử dụng trực tiếp:
```java
 @Service
 @Component
 @Repository
 @RestController
 @Entity
 @Table
 @Autowired
 @Transactional
```
trong Domain.

## 3.3. Entity

Entity phải đại diện cho một đối tượng nghiệp vụ có ý nghĩa.

Tên Entity sử dụng dạng số ít:
```text
 Employee
 Department
 Position
```
Không sử dụng:
```text
Employees
Departments
Positions
```
## 3.4. Đóng gói trạng thái

Không sử dụng Setter công khai một cách tùy tiện.

Không nên:
```java
employee.setStatus(ACTIVE);
employee.setSalary(10000000);
```
nếu việc thay đổi trạng thái có quy tắc nghiệp vụ.

Thay vào đó, Entity nên cung cấp hành vi nghiệp vụ:
```java
employee.activate();
employee.deactivate();
employee.changePosition(position);
```
Mục tiêu là Entity phải tự bảo vệ trạng thái hợp lệ của chính nó.

## 3.5. Business Rules

Các quy tắc nghiệp vụ thuộc Domain phải được thực thi tại Domain hoặc thông qua Domain Services phù hợp.

Không được đưa Business Rule quan trọng vào Controller chỉ vì Controller là nơi nhận request.

Ví dụ không nên:
```java
@PostMapping
public EmployeeResponse create(...) {


    if (employeeRepository.existsByEmail(email)) {
        ...
    }


    ...
}
```
Controller chỉ chịu trách nhiệm tiếp nhận request và chuyển yêu cầu vào Application Layer.
# 4. Application Layer
## 4.1. Mục đích

Application Layer điều phối các Use Case của hệ thống.

Ví dụ:
```
Create Employee
Update Employee
Deactivate Employee
Get Employee
Search Employees
```
Application Layer không chứa chi tiết kỹ thuật của database, HTTP hoặc Framework.

## 4.2. Use Case

Mỗi Use Case nên có trách nhiệm rõ ràng.

Không tạo một Application Service khổng lồ xử lý quá nhiều nghiệp vụ không liên quan.

Ví dụ:
```
CreateEmployeeService
UpdateEmployeeService
DeactivateEmployeeService
GetEmployeeService
```
thay vì:
```
EmployeeService
```
chứa toàn bộ logic của Employee.

## 4.3. Input Port

Input Port định nghĩa cách Application Layer nhận yêu cầu từ bên ngoài.

Ví dụ:
```java
public interface CreateEmployeeUseCase {


    CreateEmployeeResult execute(CreateEmployeeCommand command);
}
```
Infrastructure có thể gọi Input Port này.

## 4.4. Output Port

Output Port định nghĩa những gì Application Layer cần từ bên ngoài.

Ví dụ:
```java
public interface EmployeeRepository {


    Optional<Employee> findById(EmployeeId id);


    void save(Employee employee);
}
```
Application không được biết repository đang sử dụng:
```
JPA
Hibernate
MySQL
JDBC
```
Việc triển khai interface thuộc Infrastructure Layer.

## 4.5. Spring Annotation

Application Layer không sử dụng trực tiếp các Spring Annotation như:
```java
@Service
@Component
@Autowired
```
Việc đăng ký Bean và Dependency Injection được cấu hình tại Infrastructure Layer.
# 5. Infrastructure Layer

Infrastructure là ranh giới giữa hệ thống và các công nghệ bên ngoài.

Tầng này chịu trách nhiệm tích hợp:

- Spring Boot
- REST API
- Spring Security
- JPA
- Hibernate
- MySQL
- Flyway
- External Services
- Configuration

## 5.1. REST Controller

Controller chỉ chịu trách nhiệm:

- Nhận HTTP request.
- Validate input ở mức request.
- Chuyển request thành Command/Query.
- Gọi Input Port.
- Chuyển Result thành HTTP Response.

Controller không được chứa Business Logic.

Luồng chuẩn:
```
HTTP Request
     ↓
Controller
     ↓
Input Port
     ↓
Application Service
     ↓
Domain
     ↓
Output Port
     ↓
Infrastructure Adapter
     ↓
Database
```
## 5.2. Repository Adapter

JPA Repository chỉ thuộc Infrastructure.

Ví dụ:
```
Application
    ↓
EmployeeRepository
    ↑
JpaEmployeeRepositoryAdapter
    ↓
Spring Data JPA
    ↓
MySQL
```
Application chỉ biết interface.

Application không biết implementation sử dụng JPA.
# 6. Mapping

Phải phân biệt rõ các loại Model:
```
Domain Model
Persistence Model
Request DTO
Response DTO
Command
Query
Result
```
Không sử dụng một class duy nhất cho tất cả mục đích.

Ví dụ không nên dùng:
```java
EmployeeEntity
```
đồng thời làm:
```
JPA Entity
Domain Entity
Request DTO
Response DTO
```
## 6.1. Persistence Mapping

Phải có mapping rõ ràng:
```
JPA Entity
    ↕
Domain Entity
```
## 6.2. API Mapping

Request/Response phải được chuyển đổi rõ ràng:
```
HTTP Request
    ↓
Request DTO
    ↓
Command
    ↓
Application
```
và:
```
Application Result
    ↓
Response DTO
    ↓
HTTP Response
```
# 7. Transaction

Transaction phải được quản lý ở Infrastructure / Application Boundary phù hợp với kiến trúc.

Không đặt @Transactional trong Domain Layer.

Không sử dụng Transaction một cách tùy tiện.

Mỗi Transaction phải có phạm vi rõ ràng và phục vụ một Use Case.

Ví dụ:
```
Create Employee
    ├── Validate
    ├── Create Domain Entity
    ├── Save Employee
    └── Commit Transaction
```
# 8. Validation

Phân biệt hai loại validation:

## 8.1. Input Validation

Kiểm tra dữ liệu request:
```
Email có đúng định dạng?
Tên có bị bỏ trống?
Số điện thoại có đúng format?
```
Có thể thực hiện ở API boundary bằng Bean Validation.

### 8.2. Business Validation

Kiểm tra quy tắc nghiệp vụ:
```
Employee đã tồn tại?
Employee có được phép chuyển Department?
Employee đang ở trạng thái nào?
```
Business Validation phải thuộc Application/Domain tùy theo bản chất của quy tắc.

Không đưa toàn bộ Business Validation vào Controller.

# 9. Exception Handling

Không được nuốt Exception.

Không sử dụng:
```java
try {
    ...
} catch (Exception e) {
    // bỏ qua
}
```
mà không có lý do chính đáng.

Các lỗi nghiệp vụ cần có Exception rõ nghĩa.

Ví dụ:
```
EmployeeNotFoundException
EmployeeAlreadyExistsException
InvalidEmployeeStatusException
```
Exception phải được xử lý tập trung ở API boundary và chuyển thành Response phù hợp.

Ví dụ:
```json
{
  "code": "EMPLOYEE_NOT_FOUND",
  "message": "Không tìm thấy nhân viên"
}
```
Không trả stack trace hoặc thông tin nội bộ cho client.

# 10. Security

Security là một phần của Infrastructure Layer.

Các thành phần như:
```
Spring Security
JWT
Authentication Filter
Security Configuration
Password Encoder
```
không được làm Domain phụ thuộc vào Spring Security.

Domain chỉ nên quan tâm đến nghiệp vụ liên quan đến quyền hoặc trạng thái nếu nghiệp vụ thực sự yêu cầu.

Authorization phải được kiểm soát rõ ràng.

Không chỉ kiểm tra quyền ở Frontend.
```
Frontend Authorization
        +
Backend Authorization
```
Backend luôn là nơi quyết định cuối cùng.

# 11. Database

Database sử dụng:
```
MySQL
```
Schema thay đổi phải được quản lý bằng:
```
Flyway Migration
```
Không chỉnh sửa database production thủ công nếu thay đổi đó thuộc schema của hệ thống.

Mỗi migration phải:

- Có version rõ ràng.
- Có mục đích rõ ràng.
- Không sửa nội dung migration đã chạy ở môi trường dùng chung.
- Được kiểm tra trước khi merge.
# 12. Naming Convention
## Package

Sử dụng chữ thường:
```
domain
application
infrastructure
employee
department
```
## Class

PascalCase:
```
CreateEmployeeService
EmployeeController
EmployeeRepository
```
## Method

camelCase:
```
createEmployee()
findEmployeeById()
deactivateEmployee()
```
## Constant

UPPER_SNAKE_CASE:
```
MAX_LOGIN_ATTEMPTS
DEFAULT_PAGE_SIZE
```
## Boolean

Ưu tiên tên thể hiện rõ ý nghĩa:
```
isActive()
hasPermission()
canLogin()
```
# 13. Code Quality

Mã nguồn phải ưu tiên:

- Đơn giản.
- Dễ đọc.
- Dễ kiểm thử.
- Trách nhiệm rõ ràng.
- Không lặp code không cần thiết.
- Không tạo abstraction chỉ để "cho đẹp".
- Không tạo class/service quá lớn.

Không viết code chỉ để làm cho kiến trúc phức tạp hơn.

Mọi abstraction phải có lý do kỹ thuật hoặc nghiệp vụ.

# 14. Concurrency và dữ liệu duy nhất

Các nghiệp vụ liên quan đến dữ liệu duy nhất phải được bảo vệ ở nhiều lớp khi cần thiết.

Ví dụ:
```
Employee Code
Email
Username
```
Không được chỉ kiểm tra:
```java
if (!exists(email)) {
    save(employee);
}
```
và giả định rằng dữ liệu chắc chắn không trùng.

Trong trường hợp có race condition:
```
Request A ──┐
            ├── kiểm tra email
Request B ──┘
```
cả hai request có thể cùng vượt qua bước kiểm tra.

Vì vậy các ràng buộc quan trọng phải được bảo vệ thêm bằng Database Constraint/Unique Index phù hợp.

# 15. Testing

Mọi Use Case quan trọng phải có test phù hợp.

Ưu tiên kiểm thử:
```
Domain
    ↓
Application / Use Case
    ↓
Infrastructure
    ↓
API
```
Test phải kiểm tra cả:
```
Happy path.
Validation.
Business Rule.
Exception.
Boundary case.
```
Các trường hợp có thể gây sai trạng thái.

Không bỏ test chỉ để CI pass.

# 16. Cấu trúc package tham khảo

Cấu trúc cụ thể phải được điều chỉnh theo domain thực tế của hệ thống.

Ví dụ:
```
com.example.employeemanagement
│
├── domain
│   ├── employee
│   │   ├── Employee.java
│   │   ├── EmployeeId.java
│   │   └── ...
│   │
│   ├── department
│   └── ...
│
├── application
│   ├── employee
│   │   ├── create
│   │   ├── update
│   │   ├── delete
│   │   └── get
│   │
│   └── ...
│
└── infrastructure
    ├── web
    │   ├── employee
    │   └── ...
    │
    ├── persistence
    │   ├── employee
    │   └── ...
    │
    ├── security
    └── config
```
Cấu trúc domain thực tế phải được quyết định sau khi phân tích Business Domain trong tài liệu nghiệp vụ.

# 17. Nguyên tắc bắt buộc khi Code Review

Reviewer phải kiểm tra tối thiểu:

### Kiến trúc
- Dependency đi đúng hướng.
- Domain không phụ thuộc Framework.
- Application không phụ thuộc Infrastructure.
- Controller không chứa Business Logic.

### Domain
- Business Rule được đặt đúng nơi.
- Entity bảo vệ trạng thái hợp lệ.
- Không có Setter công khai không cần thiết.
### Application
- Use Case có trách nhiệm rõ ràng.
- Input/Output Port được sử dụng phù hợp.
- Không phụ thuộc trực tiếp vào JPA/HTTP.
### Infrastructure
- Controller chỉ xử lý HTTP boundary.
- Repository Adapter nằm trong Infrastructure.
- Mapping rõ ràng.
- Security nằm tại Framework Boundary.
### Database
- Thay đổi schema có Flyway Migration.
- Constraint quan trọng được bảo vệ ở Database.
- Không có migration phá vỡ dữ liệu hiện tại.
### Testing
- Test được cập nhật khi thay đổi behavior.
- Happy path đã được kiểm tra.
- Exception/Business Rule quan trọng đã được kiểm tra.
# 18. Nguyên tắc cuối cùng

Không hy sinh kiến trúc để đổi lấy tốc độ code ngắn hạn.

Mọi thay đổi lớn về:
```
Architecture
Database
Security
API Contract
Domain Model
```
phải được trao đổi và review trước khi triển khai.