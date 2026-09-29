import test from "node:test";
import assert from "node:assert/strict";
import {
  CANONICAL_ROLES,
  normalizeRoleCode,
  isRole,
  isExecutive,
  isPm,
  isRm,
  isSpecialist,
  isHr,
  isAdmin,
} from "../lib/role-utils.ts";
import { canReadProjectRoles } from "../components/rolecatalog/ProjectRoleCatalogView.tsx";
import { isDepartmentManagerRole } from "../components/department/DepartmentTree.tsx";

test("CANONICAL_ROLES contains all standard 6 role codes", () => {
  assert.equal(CANONICAL_ROLES.EXECUTIVE, "VT-01");
  assert.equal(CANONICAL_ROLES.PM, "VT-02");
  assert.equal(CANONICAL_ROLES.RM, "VT-03");
  assert.equal(CANONICAL_ROLES.SPECIALIST, "VT-04");
  assert.equal(CANONICAL_ROLES.HR, "VT-05");
  assert.equal(CANONICAL_ROLES.ADMIN, "VT-06");
});

test("normalizeRoleCode: null, undefined, empty handling", () => {
  assert.equal(normalizeRoleCode(null), "");
  assert.equal(normalizeRoleCode(undefined), "");
  assert.equal(normalizeRoleCode(""), "");
  assert.equal(normalizeRoleCode("   "), "");
});

test("normalizeRoleCode: canonical codes and compact variants", () => {
  for (let i = 1; i <= 6; i++) {
    const pad = `0${i}`;
    const canonical = `VT-${pad}`;
    assert.equal(normalizeRoleCode(canonical), canonical);
    assert.equal(normalizeRoleCode(`VT${pad}`), canonical);
    assert.equal(normalizeRoleCode(`VT_${pad}`), canonical);
    assert.equal(normalizeRoleCode(`vt-${pad}`), canonical);
    assert.equal(normalizeRoleCode(`vt${pad}`), canonical);
    assert.equal(normalizeRoleCode(`vt_${pad}`), canonical);
    assert.equal(normalizeRoleCode(`ROLE-VT-${pad}`), canonical);
    assert.equal(normalizeRoleCode(`ROLE_VT_${pad}`), canonical);
    assert.equal(normalizeRoleCode(`ROLE-VT${pad}`), canonical);
  }
});

test("normalizeRoleCode: historical aliases for VT-01 (Executive)", () => {
  const aliases = [
    "EXECUTIVE",
    "ROLE-EXECUTIVE",
    "ROLE_EXECUTIVE",
    "DIRECTOR",
    "ROLE-DIRECTOR",
    "ROLE_DIRECTOR",
    "BAN_GIAM_DOC",
    "BAN-GIAM-DOC",
    "GIAM_DOC",
    "GIAM-DOC",
  ];
  for (const alias of aliases) {
    assert.equal(normalizeRoleCode(alias), "VT-01", `Failed for alias: ${alias}`);
    assert.equal(normalizeRoleCode(alias.toLowerCase()), "VT-01", `Failed for lowercase alias: ${alias}`);
    assert.equal(isExecutive(alias), true, `isExecutive failed for: ${alias}`);
  }
});

test("normalizeRoleCode: historical aliases for VT-02 (PM)", () => {
  const aliases = [
    "PM",
    "ROLE-PM",
    "ROLE_PM",
    "PROJECT_MANAGER",
    "PROJECT-MANAGER",
    "ROLE-PROJECT-MANAGER",
    "QUAN_LY_DU_AN",
    "QUAN-LY-DU-AN",
  ];
  for (const alias of aliases) {
    assert.equal(normalizeRoleCode(alias), "VT-02", `Failed for alias: ${alias}`);
    assert.equal(normalizeRoleCode(alias.toLowerCase()), "VT-02", `Failed for lowercase alias: ${alias}`);
    assert.equal(isPm(alias), true, `isPm failed for: ${alias}`);
  }
});

test("normalizeRoleCode: historical aliases for VT-03 (RM)", () => {
  const aliases = [
    "RM",
    "ROLE-RM",
    "ROLE_RM",
    "RESOURCE_MANAGER",
    "RESOURCE-MANAGER",
    "ROLE-RESOURCE-MANAGER",
    "QUAN_LY_NGUON_LUC",
    "QUAN-LY-NGUON-LUC",
  ];
  for (const alias of aliases) {
    assert.equal(normalizeRoleCode(alias), "VT-03", `Failed for alias: ${alias}`);
    assert.equal(normalizeRoleCode(alias.toLowerCase()), "VT-03", `Failed for lowercase alias: ${alias}`);
    assert.equal(isRm(alias), true, `isRm failed for: ${alias}`);
  }
});

test("normalizeRoleCode: historical aliases for VT-04 (Specialist/Staff)", () => {
  const aliases = [
    "STAFF",
    "ROLE-STAFF",
    "ROLE_STAFF",
    "EMPLOYEE",
    "ROLE-EMPLOYEE",
    "NHAN_VIEN",
    "NHAN-VIEN",
    "DEVELOPER",
    "DEV",
    "SPECIALIST",
    "CHUYEN_VIEN",
  ];
  for (const alias of aliases) {
    assert.equal(normalizeRoleCode(alias), "VT-04", `Failed for alias: ${alias}`);
    assert.equal(normalizeRoleCode(alias.toLowerCase()), "VT-04", `Failed for lowercase alias: ${alias}`);
    assert.equal(isSpecialist(alias), true, `isSpecialist failed for: ${alias}`);
  }
});

test("normalizeRoleCode: historical aliases for VT-05 (HR)", () => {
  const aliases = [
    "HR",
    "ROLE-HR",
    "ROLE_HR",
    "HUMAN_RESOURCE",
    "HUMAN-RESOURCE",
    "HUMAN_RESOURCES",
    "HUMAN-RESOURCES",
    "NHAN_SU",
    "NHAN-SU",
  ];
  for (const alias of aliases) {
    assert.equal(normalizeRoleCode(alias), "VT-05", `Failed for alias: ${alias}`);
    assert.equal(normalizeRoleCode(alias.toLowerCase()), "VT-05", `Failed for lowercase alias: ${alias}`);
    assert.equal(isHr(alias), true, `isHr failed for: ${alias}`);
  }
});

test("normalizeRoleCode: historical aliases for VT-06 (Admin)", () => {
  const aliases = [
    "ADMIN",
    "ROLE-ADMIN",
    "ROLE_ADMIN",
    "ADMINISTRATOR",
    "ROLE-ADMINISTRATOR",
    "SYS_ADMIN",
    "SYS-ADMIN",
    "QUAN_TRI_VIEN",
    "QUAN-TRI-VIEN",
  ];
  for (const alias of aliases) {
    assert.equal(normalizeRoleCode(alias), "VT-06", `Failed for alias: ${alias}`);
    assert.equal(normalizeRoleCode(alias.toLowerCase()), "VT-06", `Failed for lowercase alias: ${alias}`);
    assert.equal(isAdmin(alias), true, `isAdmin failed for: ${alias}`);
  }
});

test("isRole matching function", () => {
  assert.equal(isRole("ROLE-ADMIN", "VT-06"), true);
  assert.equal(isRole("ADMIN", ["VT-05", "VT-06"]), true);
  assert.equal(isRole("VT-02", ["VT-01", "VT-02"]), true);
  assert.equal(isRole("PM", ["VT-01", "VT-02"]), true);
  assert.equal(isRole("VT-04", ["VT-01", "VT-02"]), false);
  assert.equal(isRole(null, "VT-01"), false);
});

test("canReadProjectRoles correctly handles valid roles, aliases, and rejects unknown roles", () => {
  assert.equal(canReadProjectRoles("VT-01"), true);
  assert.equal(canReadProjectRoles("ROLE-ADMIN"), true);
  assert.equal(canReadProjectRoles("DIRECTOR"), true);
  assert.equal(canReadProjectRoles("PM"), true);
  assert.equal(canReadProjectRoles("RM"), true);
  assert.equal(canReadProjectRoles("STAFF"), true);
  assert.equal(canReadProjectRoles("HR"), true);
  assert.equal(canReadProjectRoles("UNKNOWN_ROLE"), false);
  assert.equal(canReadProjectRoles("VT-99"), false);
  assert.equal(canReadProjectRoles(""), false);
  assert.equal(canReadProjectRoles(null), false);
  assert.equal(canReadProjectRoles(undefined), false);
});

test("isDepartmentManagerRole correctly admits manager roles and rejects VT-04 & invalid roles", () => {
  assert.equal(isDepartmentManagerRole("VT-01"), true);
  assert.equal(isDepartmentManagerRole("VT-02"), true);
  assert.equal(isDepartmentManagerRole("VT-03"), true);
  assert.equal(isDepartmentManagerRole("VT-05"), true);
  assert.equal(isDepartmentManagerRole("VT-06"), true);
  assert.equal(isDepartmentManagerRole("ROLE-ADMIN"), true);
  assert.equal(isDepartmentManagerRole("DIRECTOR"), true);
  assert.equal(isDepartmentManagerRole("PM"), true);
  assert.equal(isDepartmentManagerRole("RM"), true);
  assert.equal(isDepartmentManagerRole("HR"), true);
  assert.equal(isDepartmentManagerRole("VT-04"), false);
  assert.equal(isDepartmentManagerRole("STAFF"), false);
  assert.equal(isDepartmentManagerRole("DEVELOPER"), false);
  assert.equal(isDepartmentManagerRole("UNKNOWN_ROLE"), false);
  assert.equal(isDepartmentManagerRole("VT-99"), false);
  assert.equal(isDepartmentManagerRole(""), false);
  assert.equal(isDepartmentManagerRole(null), false);
  assert.equal(isDepartmentManagerRole(undefined), false);
});

