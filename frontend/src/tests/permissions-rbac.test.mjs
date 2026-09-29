import test from "node:test";
import assert from "node:assert/strict";
import { can, canAny, canAll, hasRole, getEffectivePermissions } from "../lib/permissions.ts";
import { canAccessTab } from "../components/dashboard/SideBar.tsx";

test("permissions helper: can, canAny, canAll, hasRole", () => {
    const user = {
        id: 10,
        username: "test_pm",
        roleCode: "VT-02",
        dataScope: "SELF",
        permissions: ["PROJECT_WBS_MANAGE", "EMPLOYEE_READ", "WORK_LOG_READ"],
    };

    assert.equal(can("PROJECT_WBS_MANAGE", user), true);
    assert.equal(can("USER_READ", user), false);
    assert.equal(can("EMPLOYEE_SKILL_APPROVE", user), false);

    assert.equal(canAny(["USER_READ", "PROJECT_WBS_MANAGE"], user), true);
    assert.equal(canAny(["USER_READ", "USER_CREATE"], user), false);

    assert.equal(canAll(["PROJECT_WBS_MANAGE", "EMPLOYEE_READ"], user), true);
    assert.equal(canAll(["PROJECT_WBS_MANAGE", "USER_READ"], user), false);

    assert.equal(hasRole("VT-02", user), true);
    assert.equal(hasRole("ROLE_VT_02", user), true);
    assert.equal(hasRole(["VT-01", "VT-02"], user), true);
    assert.equal(hasRole("VT-06", user), false);

    assert.deepEqual(getEffectivePermissions(user), ["PROJECT_WBS_MANAGE", "EMPLOYEE_READ", "WORK_LOG_READ"]);
});

test("permissions helper: null/undefined safety", () => {
    assert.equal(can("USER_READ", null), false);
    assert.equal(canAny(["USER_READ"], null), false);
    assert.equal(canAll(["USER_READ"], null), false);
    assert.equal(hasRole("VT-06", null), false);
    assert.deepEqual(getEffectivePermissions(null), []);
});

test("SideBar canAccessTab: strict attendance and project role guards", () => {
    // Admin (VT-06) must NOT see attendance
    assert.equal(canAccessTab("VT-06", "attendance"), false);
    // VT-01, VT-03, VT-05 must NOT see attendance
    assert.equal(canAccessTab("VT-01", "attendance"), false);
    assert.equal(canAccessTab("VT-03", "attendance"), false);
    assert.equal(canAccessTab("VT-05", "attendance"), false);
    // Only VT-02 and VT-04 can see attendance
    assert.equal(canAccessTab("VT-02", "attendance"), true);
    assert.equal(canAccessTab("VT-04", "attendance"), true);

    // Project is hidden from HR (VT-05) and Admin (VT-06)
    assert.equal(canAccessTab("VT-05", "project"), false);
    assert.equal(canAccessTab("VT-06", "project"), false);
    // Accessible by VT-01, VT-02, VT-03, VT-04
    assert.equal(canAccessTab("VT-01", "project"), true);
    assert.equal(canAccessTab("VT-02", "project"), true);
    assert.equal(canAccessTab("VT-03", "project"), true);
    assert.equal(canAccessTab("VT-04", "project"), true);
});
