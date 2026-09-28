import { test, describe } from "node:test";
import assert from "node:assert/strict";
import { mapAuthUser } from "../lib/api/auth.ts";

describe("P1-3: Auth Account Real Data Mapping & Fallback Tests", () => {
  // Base DTO template
  const createBaseDto = (overrides = {}) => ({
    id: 1,
    username: "nv01",
    email: "nv01@example.com",
    roleCode: "VT-04",
    roleName: "Nhân viên chuyên môn",
    status: "ACTIVE",
    employeeId: 5,
    employeeCode: "NV-DEV01",
    fullName: "Nguyễn Văn Dev",
    orgUnitId: 1,
    orgUnitName: "Ban Phát triển Phần mềm",
    dataScope: "SELF",
    scopeOrgUnitId: null,
    permissions: ["WORK_LOG_VIEW", "WORK_LOG_CREATE"],
    ...overrides,
  });

  // UI rendering helper identical to Dashboard & Timesheet logic
  const renderAccountOverview = (user) => ({
    employeeCode: user?.employeeCode || "—",
    fullName: user?.fullName || user?.username || "—",
    department: user?.orgUnitName || "—",
    roleName: user?.roleName || "—",
    email: user?.email || "—",
  });

  const renderTimesheetBadge = (user) => ({
    codeLabel: `Mã: ${user?.employeeCode || "—"}`,
  });

  test("Case 1 — Có đầy đủ dữ liệu từ /auth/me (nv01)", () => {
    const dto = createBaseDto({
      employeeId: 5,
      employeeCode: "NV-DEV01",
      email: "nv01@example.com",
    });

    const authUser = mapAuthUser(dto);

    assert.equal(authUser.employeeCode, "NV-DEV01");
    assert.equal(authUser.email, "nv01@example.com");
    assert.notEqual(authUser.employeeCode, "EMP-5");
    assert.notEqual(authUser.employeeCode, "EMP5");
    assert.notEqual(authUser.employeeCode, "NV001");
    assert.notEqual(authUser.email, "employee@company.com");

    const ui = renderAccountOverview(authUser);
    assert.equal(ui.employeeCode, "NV-DEV01");
    assert.equal(ui.email, "nv01@example.com");

    const timesheet = renderTimesheetBadge(authUser);
    assert.equal(timesheet.codeLabel, "Mã: NV-DEV01");
  });

  test("Case 2 — Thiếu employeeCode trong API response", () => {
    const dto = createBaseDto({
      employeeId: null,
      employeeCode: null,
      email: "nv01@example.com",
    });

    const authUser = mapAuthUser(dto);

    assert.equal(authUser.employeeCode, null);
    assert.notEqual(authUser.employeeCode, "EMP-001");
    assert.notEqual(authUser.employeeCode, "EMP5");
    assert.notEqual(authUser.employeeCode, "EMP-5");

    const ui = renderAccountOverview(authUser);
    assert.equal(ui.employeeCode, "—");
    assert.notEqual(ui.employeeCode, "NV001");
    assert.notEqual(ui.employeeCode, "EMP-5");

    const timesheet = renderTimesheetBadge(authUser);
    assert.equal(timesheet.codeLabel, "Mã: —");
    assert.ok(!timesheet.codeLabel.includes("EMP-5"));
    assert.ok(!timesheet.codeLabel.includes("NV001"));
  });

  test("Case 3 — Thiếu email trong API response", () => {
    const dto = createBaseDto({
      email: null,
      employeeCode: "NV-DEV01",
    });

    const authUser = mapAuthUser(dto);

    assert.equal(authUser.email, null);
    assert.notEqual(authUser.email, "employee@company.com");
    assert.notEqual(authUser.email, "nv01@hrm.local");

    const ui = renderAccountOverview(authUser);
    assert.equal(ui.email, "—");
    assert.notEqual(ui.email, "employee@company.com");
    assert.notEqual(ui.email, "nv01@hrm.local");
  });

  test("Case 4 — EmployeeId có giá trị nhưng employeeCode thiếu (không được sinh EMP-5)", () => {
    const dto = createBaseDto({
      employeeId: 5,
      employeeCode: null,
    });

    const authUser = mapAuthUser(dto);

    assert.equal(authUser.employeeCode, null);
    assert.notEqual(authUser.employeeCode, "EMP-5");
    assert.notEqual(authUser.employeeCode, "EMP5");
    assert.notEqual(authUser.employeeCode, "EMP005");

    const ui = renderAccountOverview(authUser);
    assert.equal(ui.employeeCode, "—");
    assert.notEqual(ui.employeeCode, "EMP-5");
    assert.notEqual(ui.employeeCode, "NV001");

    const timesheet = renderTimesheetBadge(authUser);
    assert.equal(timesheet.codeLabel, "Mã: —");
    assert.notEqual(timesheet.codeLabel, "Mã: EMP-5");
    assert.notEqual(timesheet.codeLabel, "Mã: #5");
  });

  test("Regression: Dashboard và Timesheet hiển thị nhất quán dữ liệu thật của nv01", () => {
    const dto = createBaseDto({
      username: "nv01",
      employeeId: 5,
      employeeCode: "NV-DEV01",
      email: "nv01@example.com",
    });

    const authUser = mapAuthUser(dto);
    const dashboard = renderAccountOverview(authUser);
    const timesheet = renderTimesheetBadge(authUser);

    // Dashboard verification
    assert.equal(dashboard.employeeCode, "NV-DEV01");
    assert.equal(dashboard.email, "nv01@example.com");

    // Timesheet verification
    assert.equal(timesheet.codeLabel, "Mã: NV-DEV01");

    // Ensure forbidden hard-coded strings never appear
    const serializedDashboard = JSON.stringify(dashboard);
    assert.ok(!serializedDashboard.includes("NV001"));
    assert.ok(!serializedDashboard.includes("employee@company.com"));
    assert.ok(!serializedDashboard.includes("EMP-5"));
    assert.ok(!serializedDashboard.includes("EMP5"));

    const serializedTimesheet = JSON.stringify(timesheet);
    assert.ok(!serializedTimesheet.includes("EMP-5"));
    assert.ok(!serializedTimesheet.includes("NV001"));
    assert.ok(!serializedTimesheet.includes("#5"));
  });
});
