# 🛡️ AI BACKEND DEVELOPMENT GUIDELINE

> **System Target**: Employee Management System (Resource Planning)
> **Role & Purpose**: Mandatory architecture & coding standards for AI Agents generating and editing Backend Java code.

---

## 1. Tech Stack Requirements

- **Java Version**: Java 21
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Persistence**: Spring Data JPA + Hibernate + MySQL
- **Database Migrations**: Flyway (in `src/main/resources/db/migration`)
- **Security**: Spring Security + JWT

---

## 2. Architecture Pattern: Hexagonal Architecture + DDD

```text
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE                           │
│ (REST Controller, JPA Entity, Repositories, Security, Config) │
│                                                             │
│        ┌─────────────────────────────────────────┐          │
│        │             APPLICATION                 │          │
│        │ (Use Cases, Ports, Commands, Queries)   │          │
│        │                                         │          │
│        │       ┌───────────────────────┐         │          │
│        │       │        DOMAIN         │         │          │
│        │       │ (Entities, Value Obj) │         │          │
│        │       └───────────────────────┘         │          │
│        └─────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

### Strict Dependency Rules:
- **Infrastructure → Application → Domain**
- **NEVER ALLOW**:
  - `Domain → Application`
  - `Domain → Infrastructure`
  - `Application → Infrastructure`

---

## 3. Layer Enforcement Guidelines

### 3.1. Domain Layer (`domain`)
- **Pure Java ONLY**: No Spring annotations (`@Service`, `@Component`, `@Repository`, `@Entity`, `@Transactional`, `@Autowired`).
- **Encapsulation**: Do NOT expose public setters arbitrarily (`setStatus(...)`). Use domain methods (`activate()`, `deactivate()`, `changePosition()`).
- **Entities & Value Objects**: Represent business concepts, ensure invariants. Singular names (e.g., `Employee`, `Department`).

### 3.2. Application Layer (`application`)
- **Use Cases**: Single responsibility services (e.g. `CreateEmployeeService`, `DeactivateEmployeeService`).
- **Input Ports**: Interfaces defining inbound use cases (e.g., `CreateEmployeeUseCase`).
- **Output Ports**: Interfaces defining outbound needs (e.g., `EmployeeRepository`). No JPA/Database details in interfaces.
- **DTOs**: Use Commands (`CreateEmployeeCommand`) and Queries (`GetEmployeeQuery`). No direct Spring annotations in Application interfaces.

### 3.3. Infrastructure Layer (`infrastructure`)
- **Web Controllers**: Handle HTTP requests/responses, input validation (Bean Validation), convert to Commands/Queries, delegate to Input Ports. **Zero business logic in controllers**.
- **Persistence Adapters**: Implements Application Output Ports (e.g. `JpaEmployeeRepositoryAdapter` implementing `EmployeeRepository`). Map JPA Entities ↔ Domain Entities.
- **Security & Config**: Spring Security configuration, JWT filters, Spring `@Configuration` and Bean definitions.

---

## 4. Business Rules Enforcement (QTN-01 to QTN-24)

AI Agents MUST strictly implement business rules in Domain / Application layers:
- **QTN-01**: Data access scoped by role & organizational unit.
- **QTN-02**: Audit log required when reading/exporting employee data.
- **QTN-04**: Tasks and allocations allowed only on active projects.
- **QTN-05**: Do not assign or allocate deactivated/resigned employees.
- **QTN-06**: Warn PM when actual hours exceed 80% of budget.
- **QTN-07**: Approved timesheets are immutable (adjustments only with reasons).
- **QTN-08**: Closed projects reject new hours/allocations.
- **QTN-09**: Max 12 hours/day per employee in timesheet.
- **QTN-10**: `Available Capacity = Standard Hours - Holidays - Approved Leaves`.
- **QTN-11**: Total weekly allocation cannot exceed available capacity (warn on overload).
- **QTN-12**: Overload (>100% capacity) flagged with visual warning & excess hours.
- **QTN-13**: Soft booking hours tracked separately from confirmed allocations.
- **QTN-14**: Simulation scenarios isolated in sandbox until applied.
- **QTN-15**: Allocation changes must record audit log and notify PM.
- **QTN-16**: One effective standard hours record per employee at any time.
- **QTN-17**: WBS task dependencies must form a Directed Acyclic Graph (DAG) - no cycles.
- **QTN-18**: Locked allocation periods prohibit edits unless unlocked.
- **QTN-19**: Prevent duplicate event notifications.
- **QTN-20**: Database restore restricted to Admin.
- **QTN-21**: Outsource employee allocation bounded by contract duration.
- **QTN-22**: Approved leaves can only be cancelled before start date.
- **QTN-23**: Overload/idle thresholds configured dynamically by leadership.
- **QTN-24**: Employee confirmation is feedback only; does not mutate allocation directly.

---

## 5. Coding Standards & Conventions

- **Packages**: `com.hrm.employeemanagement.domain`, `com.hrm.employeemanagement.application`, `com.hrm.employeemanagement.infrastructure`
- **Exception Handling**: Use domain-specific exceptions (`EmployeeNotFoundException`, `ResourceOverloadedException`). Handle globally at `@ControllerAdvice`.
- **Flyway**: All database schema changes MUST use versioned Flyway scripts (`V1__...sql`, `V2__...sql`). Never alter production tables manually.
- **Concurrency**: Use Database constraints / Unique Indexes for unique checks (`employee_code`, `email`) to prevent race conditions.
