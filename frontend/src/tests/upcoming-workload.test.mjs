import { test, describe } from "node:test";
import assert from "node:assert/strict";

// Helper functions mirroring business logic in UpcomingWorkloadView and backend service
export function calculateNetAvailableHours(standardHours, holidayHours = 0, approvedLeaveHours = 0) {
    return Math.max(0, standardHours - holidayHours - approvedLeaveHours);
}

export function calculateUtilization(allocatedHours, netAvailableHours) {
    if (netAvailableHours <= 0) {
        return allocatedHours > 0 ? 100 : 0;
    }
    return Math.round((allocatedHours / netAvailableHours) * 1000) / 10;
}

export function getWorkloadStatus(utilizationRate, overloadThreshold = 100, idleThreshold = 70) {
    if (utilizationRate > overloadThreshold) {
        return "OVERLOADED";
    }
    if (utilizationRate < idleThreshold) {
        return "IDLE";
    }
    return "NORMAL";
}

export function getStatusColorClass(status) {
    switch (status) {
        case "OVERLOADED":
            return {
                bar: "bg-red-500",
                badge: "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-400 border-red-200 dark:border-red-800",
                border: "border-red-300 dark:border-red-800"
            };
        case "IDLE":
            return {
                bar: "bg-amber-500",
                badge: "bg-amber-100 text-amber-700 dark:bg-amber-900/40 dark:text-amber-400 border-amber-200 dark:border-amber-800",
                border: "border-amber-300 dark:border-amber-800"
            };
        case "NORMAL":
        default:
            return {
                bar: "bg-emerald-500",
                badge: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800",
                border: "border-emerald-300 dark:border-emerald-800"
            };
    }
}

export function canAccessEmployeeWorkload(currentUser, targetEmployee) {
    if (!currentUser) return false;
    
    // Admin / Director / HR with COMPANY scope
    if (currentUser.dataScope === "COMPANY" || currentUser.roleCode === "VT-01" || currentUser.roleCode === "VT-02") {
        return true;
    }
    
    // Resource Manager with ORGANIZATION_BRANCH scope
    if (currentUser.dataScope === "ORGANIZATION_BRANCH" || currentUser.roleCode === "VT-03") {
        return currentUser.scopeOrgUnitId === targetEmployee.orgUnitId;
    }
    
    // Specialist / Default with SELF scope
    return currentUser.employeeId === targetEmployee.id;
}

describe("NCL-13-CN-004: Upcoming Workload Frontend Logic Tests", () => {

    test("TC-01: Luồng thành công - Tính toán khối lượng 8 tuần bình thường và năng lực khả dụng", () => {
        const standardHours = 40;
        const holidayHours = 0;
        const leaveHours = 0;
        const netHours = calculateNetAvailableHours(standardHours, holidayHours, leaveHours);
        assert.equal(netHours, 40, "Năng lực khả dụng là 40h khi không có nghỉ lễ/nghỉ phép");

        const allocatedHours = 36;
        const utilization = calculateUtilization(allocatedHours, netHours);
        assert.equal(utilization, 90.0, "Mức độ sử dụng là 90%");

        const status = getWorkloadStatus(utilization, 100, 70);
        assert.equal(status, "NORMAL", "90% nằm trong khoảng bình thường (70% - 100%)");

        const colors = getStatusColorClass(status);
        assert.ok(colors.bar.includes("emerald"), "Trạng thái NORMAL sử dụng màu xanh emerald");
    });

    test("TC-02: Ngoại lệ - Một tuần vượt ngưỡng quá tải (Overloaded Week)", () => {
        const standardHours = 40;
        const holidayHours = 0;
        const leaveHours = 0;
        const netHours = calculateNetAvailableHours(standardHours, holidayHours, leaveHours);

        const allocatedHours = 48; // 48h / 40h = 120%
        const utilization = calculateUtilization(allocatedHours, netHours);
        assert.equal(utilization, 120.0, "Mức độ sử dụng 120%");

        const status = getWorkloadStatus(utilization, 100, 70);
        assert.equal(status, "OVERLOADED", "120% > 100% phải cảnh báo OVERLOADED");

        const overloadHours = allocatedHours - (netHours * 100 / 100);
        assert.equal(overloadHours, 8, "Vượt quá ngưỡng 8 giờ");

        const colors = getStatusColorClass(status);
        assert.ok(colors.bar.includes("red"), "Trạng thái OVERLOADED sử dụng màu đỏ cảnh báo");
    });

    test("TC-03: Tuần có nghỉ lễ hoặc nghỉ phép (Giảm năng lực khả dụng)", () => {
        const standardHours = 40;
        const holidayHours = 8; // 1 ngày nghỉ lễ
        const leaveHours = 8;   // 1 ngày nghỉ phép
        const netHours = calculateNetAvailableHours(standardHours, holidayHours, leaveHours);
        assert.equal(netHours, 24, "40h - 8h lễ - 8h phép = 24h khả dụng");

        const allocatedHours = 24;
        const utilization = calculateUtilization(allocatedHours, netHours);
        assert.equal(utilization, 100.0, "24h / 24h = 100%");

        const status = getWorkloadStatus(utilization, 100, 70);
        assert.equal(status, "NORMAL", "100% bằng ngưỡng trần là NORMAL");
    });

    test("TC-04: Ngưỡng quá tải tùy biến (Custom Overload Threshold: 110%)", () => {
        const netHours = 40;
        const allocatedHours = 42; // 42h / 40h = 105%
        const utilization = calculateUtilization(allocatedHours, netHours);
        assert.equal(utilization, 105.0);

        // Với ngưỡng mặc định 100% -> OVERLOADED
        const statusDefault = getWorkloadStatus(utilization, 100, 70);
        assert.equal(statusDefault, "OVERLOADED");

        // Với ngưỡng cấu hình 110% -> NORMAL
        const statusCustom = getWorkloadStatus(utilization, 110, 70);
        assert.equal(statusCustom, "NORMAL", "105% <= 110% nên là NORMAL");
    });

    test("Tuần rảnh rỗi (Idle Week): utilization < idleThreshold (70%)", () => {
        const netHours = 40;
        const allocatedHours = 20; // 50%
        const utilization = calculateUtilization(allocatedHours, netHours);
        assert.equal(utilization, 50.0);

        const status = getWorkloadStatus(utilization, 100, 70);
        assert.equal(status, "IDLE", "50% < 70% được phân loại là IDLE");

        const colors = getStatusColorClass(status);
        assert.ok(colors.bar.includes("amber"), "Trạng thái IDLE sử dụng màu vàng amber");
    });

    test("Phân quyền DataScope: SELF chỉ xem được của chính mình, RM xem được chi nhánh, HR/Admin xem toàn công ty", () => {
        const employeeSelf = { id: 10, orgUnitId: "ORG-01" };
        const employeeOtherBranch = { id: 20, orgUnitId: "ORG-02" };
        const employeeSameBranch = { id: 30, orgUnitId: "ORG-01" };

        const specialistUser = { employeeId: 10, dataScope: "SELF", roleCode: "VT-04", scopeOrgUnitId: null };
        const rmUser = { employeeId: 99, dataScope: "ORGANIZATION_BRANCH", roleCode: "VT-03", scopeOrgUnitId: "ORG-01" };
        const adminUser = { employeeId: 1, dataScope: "COMPANY", roleCode: "VT-01", scopeOrgUnitId: null };

        // Specialist
        assert.equal(canAccessEmployeeWorkload(specialistUser, employeeSelf), true, "Specialist xem được chính mình");
        assert.equal(canAccessEmployeeWorkload(specialistUser, employeeSameBranch), false, "Specialist không thể xem người khác");

        // RM
        assert.equal(canAccessEmployeeWorkload(rmUser, employeeSameBranch), true, "RM xem được nhân sự cùng chi nhánh");
        assert.equal(canAccessEmployeeWorkload(rmUser, employeeOtherBranch), false, "RM không thể xem nhân sự khác chi nhánh");

        // Admin / Company
        assert.equal(canAccessEmployeeWorkload(adminUser, employeeSelf), true, "Admin xem được bất kỳ nhân viên nào");
        assert.equal(canAccessEmployeeWorkload(adminUser, employeeOtherBranch), true, "Admin xem được chi nhánh khác");
    });
});
