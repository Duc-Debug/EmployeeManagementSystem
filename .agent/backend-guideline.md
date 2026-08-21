# 🛡️ AI BACKEND DEVELOPMENT GUIDELINE

> **System Target**: Employee Management System (Resource Planning)
> **Role & Purpose**: Mandatory architecture & coding standards for AI Agents generating and editing Backend Java code.

---

## 1. Tech Stack Requirements

- **Java Version**: Java 21
- **Framework**: Spring Boot 3.x / 4.x
- **Build Tool**: Maven
- **Persistence**: Spring Data JPA + Hibernate + MySQL
- **Database Migrations**: Flyway (in `src/main/resources/db/migration`)
- **Security**: Spring Security + JWT

---

## 2. Architecture Pattern: Clean Architecture + Hexagonal (Ports & Adapters) + DDD

```text
domain
  ↑
application
  ↑
infrastructure
```

### Strict Dependency Rules:
- **Infrastructure → Application → Domain**
- **CORE PRINCIPLE**: Domain does NOT know Spring, JPA, HTTP, JWT, MySQL, or any framework/infrastructure.

---

## 3. Standard Package Structure

```text
src/main/java/com/hrm/employeemanagement/
├── domain/
│   ├── [concept]/                      # e.g., orgunit/, user/, employee/
│   │   ├── Entity.java
│   │   ├── ValueObject.java (e.g. OrgUnitId.java)
│   │   └── Enum.java
│   ├── exception/
│   │   └── [concept]/                  # Business exceptions
│   │       └── SpecificException.java
│   └── policy/                         # Pure domain policy logic
│       └── DomainPolicy.java
│
├── application/
│   ├── dto/
│   │   └── [concept]/                  # Commands, Queries, Results
│   │       ├── CreateCommand.java
│   │       └── Result.java
│   ├── service/
│   │   └── [concept]/                  # Application Services
│   │       └── ConceptService.java
│   └── port/
│       ├── inbound/
│       │   └── [concept]/              # Use Case interfaces
│       │       └── CreateUseCase.java
│       └── outbound/
│           └── [concept]/              # Ports needed by Application
│               ├── LoadPort.java
│               └── SavePort.java
│
└── infrastructure/
    ├── adapter/
    │   ├── inbound/
    │   │   └── web/
    │   │       └── [concept]/          # Controllers & Web DTOs
    │   │           ├── Controller.java
    │   │           └── dto/
    │   │               └── Request.java
    │   └── outbound/
    │       ├── persistence/
    │       │   └── [concept]/          # JPA Entity, Repository, Adapter, Mapper
    │       │       ├── entity/
    │       │       │   └── JpaEntity.java
    │       │       ├── repository/
    │       │       │   └── SpringDataRepository.java
    │       │       ├── RepositoryAdapter.java
    │       │       └── PersistenceMapper.java
    │       └── security/
    │           └── SecurityAdapters.java
    ├── config/                         # Wiring & Configuration
    ├── transaction/
    │   └── [concept]/                  # Transactional Decorators
    └── security/                       # Security Filters & Principal
```

---

## 4. Layer Enforcement Guidelines

### 4.1. Domain Layer (`domain/`)
- **Pure Java ONLY**: No Spring annotations (`@Service`, `@Component`, `@Repository`, `@Entity`, `@Transactional`, `@Autowired`).
- **Value Objects**: Essential IDs must be Value Objects (e.g., `public record OrgUnitId(Long value) {}`).
- **Encapsulation**: Do NOT expose public setters. Use business behavior methods (`activate()`, `deactivate()`, `changeParent()`).
- **Domain Exceptions**: Located in `domain/exception/[concept]/`. Name exceptions after business rules broken (`DuplicateUnitCodeException`).

### 4.2. Application Layer (`application/`)
- **Ports & Terminology**: Use `port/inbound/[concept]/` for Use Cases (e.g. `CreateOrgUnitUseCase`) and `port/outbound/[concept]/` for outbound ports (`LoadOrgUnitPort`, `SaveOrgUnitPort`).
- **Services**: Located in `application/service/[concept]/` (e.g. `OrgUnitService`).
- **DTOs**: Located in `application/dto/[concept]/` (e.g. `CreateOrgUnitCommand`, `OrgUnitResult`).

### 4.3. Infrastructure Layer (`infrastructure/`)
- **Inbound Web Adapters**: Located in `infrastructure/adapter/inbound/web/[concept]/` (Controllers and Request/Response DTOs).
- **Outbound Persistence Adapters**: Located in `infrastructure/adapter/outbound/persistence/[concept]/` (containing `entity/`, `repository/`, `RepositoryAdapter`, `PersistenceMapper`).
- **Transactions**: Separated under `infrastructure/transaction/[concept]/` via Decorators or Transactional wrappers. Domain and Application do not depend on Spring `@Transactional`.

---

## 5. Business Rules Enforcement (QTN-01 to QTN-24)

- **QTN-01**: Data access scoped by role & organizational unit.
- **BR-ORG-01**: Unique unit code check.
- **BR-ORG-02**: Cyclic dependency prevention (Non-cyclic graph check).
- **BR-ORG-03**: Soft delete only for units with historical data (`INACTIVE`).
- **BR-ORG-04**: Inactive units reject new allocations.
