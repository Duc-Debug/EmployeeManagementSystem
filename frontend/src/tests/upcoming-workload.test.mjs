import { test, describe } from "node:test";
import assert from "node:assert/strict";
import {
    getISOWeeksInYear,
    calculateNetAvailableHours,
    calculateUtilization,
    getWorkloadStatus,
    getStatusColorClass,
    isSpecialistRole,
} from "../components/workload/workloadUtils.ts";

export function canAccessEmployeeWorkload(currentUser, targetEmployee) {
    if (!currentUser || !isSpecialistRole(currentUser.roleCode)) return false;
    return currentUser.employeeId === targetEmployee.id;
}

describe("NCL-13-CN-004: Upcoming Workload Frontend Logic Tests", () => {

    test("ISO Weeks in year: Tính đúng năm có 52 và 53 tuần ISO", () => {
        assert.equal(getISOWeeksInYear(2025), 52, "2025 có 52 tuần ISO");
        assert.equal(getISOWeeksInYear(2026), 53, "2026 có 53 tuần ISO");
        assert.equal(getISOWeeksInYear(2027), 52, "2027 có 52 tuần ISO");
    });

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

    test("Phân quyền: Chỉ nhân viên chuyên môn (VT-04) mới có quyền truy cập khối lượng công việc của chính mình", () => {
        const employeeSelf = { id: 10, orgUnitId: "ORG-01" };
        const employeeOther = { id: 20, orgUnitId: "ORG-01" };

        const specialistUser = { employeeId: 10, dataScope: "SELF", roleCode: "VT-04" };
        const rmUser = { employeeId: 99, dataScope: "ORGANIZATION_BRANCH", roleCode: "VT-03" };
        const adminUser = { employeeId: 1, dataScope: "COMPANY", roleCode: "VT-01" };

        // Specialist
        assert.equal(isSpecialistRole("VT-04"), true, "VT-04 là chuyên môn");
        assert.equal(canAccessEmployeeWorkload(specialistUser, employeeSelf), true, "Specialist xem được chính mình");
        assert.equal(canAccessEmployeeWorkload(specialistUser, employeeOther), false, "Specialist không thể xem người khác");

        // Non-specialist (RM, Admin/BGĐ) cannot access this feature view
        assert.equal(isSpecialistRole("VT-03"), false, "VT-03 không phải chuyên môn");
        assert.equal(isSpecialistRole("VT-01"), false, "VT-01 không phải chuyên môn");
        assert.equal(canAccessEmployeeWorkload(rmUser, employeeSelf), false, "RM không được truy cập tính năng chuyên môn này");
        assert.equal(canAccessEmployeeWorkload(adminUser, employeeSelf), false, "Admin/BGĐ không truy cập tính năng chuyên môn cá nhân này");
    });
});
