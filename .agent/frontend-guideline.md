# 🎨 AI FRONTEND DEVELOPMENT GUIDELINE

> **System Target**: Employee Management System (Resource Planning)
> **Role & Purpose**: Mandatory architecture & coding standards for AI Agents generating and editing Frontend React/TypeScript code.

---

## 1. Tech Stack Requirements

- **Framework**: React 18+ (Vite)
- **Language**: TypeScript (Strict Mode)
- **Routing**: React Router DOM v6+
- **Styling**: Tailwind CSS / CSS Modules
- **Linting**: ESLint + Prettier

---

## 2. Architecture & Layering Pattern

```text
┌─────────────────────────────────────────────┐
│                   UI                        │
│ (Pages, Layouts, View Components)           │
├─────────────────────────────────────────────┤
│              APPLICATION                    │
│ (Custom Hooks, State Management, UI Logic)  │
├─────────────────────────────────────────────┤
│               DATA ACCESS                   │
│ (API Client, Services)                      │
├─────────────────────────────────────────────┤
│                 TYPES                       │
│ (Domain Models, DTO Requests/Responses)     │
└─────────────────────────────────────────────┘
```

### Flow Enforcement:
`Page` → `Component` → `Custom Hook` → `API Service` → `HTTP Client` → `Backend`

**NEVER ALLOW**:
- Direct `fetch()` or `axios` calls inside JSX Components.
- Embedding complex business validation directly inside render functions.
- Storing server data in global state when a feature hook can manage it.

---

## 3. Directory & Feature Organization

Organize code by **Feature/Domain**:

```text
src/
├── features/
│   ├── employee/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── services/
│   │   ├── types/
│   │   └── pages/
│   ├── resource-allocation/
│   ├── project-wbs/
│   └── timesheet/
├── components/
│   ├── common/        # Shared UI (Button, Modal, Table, Input)
│   └── layout/        # Header, Sidebar, AppLayout
├── routes/
├── services/          # Base Axios/Fetch HTTP Client
└── types/             # Shared Types
```

---

## 4. Key Rules for AI Code Generation

### 4.1. Single Responsibility Components
- Break UI into granular components (`EmployeePage` → `EmployeeTable` → `EmployeeRow`).
- Keep components focused on presentation. Abstract state and side-effects into custom hooks.

### 4.2. TypeScript Precision
- **NO `any` types**. Explicitly type all props, state, API requests, and responses.
- Define separate Request and Response interfaces (e.g., `CreateEmployeeRequest`, `EmployeeResponse`).

### 4.3. State Management Rules
- **Local State (`useState`)**: Modals, tabs, local filter toggles.
- **Form State**: Form inputs & validation.
- **Server State**: Managed via dedicated API hooks (`useEmployees`, `useProjects`).
- **Global State**: Authentication, active user profile, theme settings.

### 4.4. UI States & UX Constraints
Every data-fetching UI MUST handle 4 distinct states:
1. **Loading** (Skeleton or Spinner)
2. **Success** (Rendered Content)
3. **Empty** (User-friendly Empty State)
4. **Error** (User-friendly Error Alert, no raw stack traces)

### 4.5. Business Rules Compliance in UI
- **Capacity Warnings**: Highlight over-allocated weeks (>100% capacity) with visual badge/color.
- **Timesheet Immutable Rules**: Approved entries must disable inline edit controls and offer "Request Adjustment".
- **DAG Dependency Graph**: Task creation UI must prevent cyclic selections.
- **Simulation Sandbox**: Display explicit "Simulation Mode" banner when editing draft scenarios.
