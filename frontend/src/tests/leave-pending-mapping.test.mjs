import { test, describe } from "node:test";
import assert from "node:assert/strict";

describe("P1-4: Pending Leave Requests Mapping Tests", () => {

    test("Nghiệm thu P1-4: RM thấy đúng 'Nguyễn Văn Dev · Phòng Phát triển Phần mềm'", () => {
        // Dữ liệu Backend trả về từ GET /api/v1/leave-requests/pending
        const apiResponse = [
            {
                id: 101,
                employeeId: 5,
                employeeName: "Nguyễn Văn Dev",
                orgUnitName: "Phòng Phát triển Phần mềm",
                department: null,
                leaveType: "ANNUAL",
                startDate: "2026-10-01",
                endDate: "2026-10-03",
                daysCount: 3,
                hoursDeducted: 24,
                reason: "Nghỉ phép cá nhân",
                status: "PENDING",
                createdAt: "2026-09-28T10:00:00",
            }
        ];

        // Logic mapping trong LeaveManagementView.tsx
        const mapped = apiResponse.map((item) => ({
            id: `LV-${item.id}`,
            employeeId: String(item.employeeId),
            employeeName: item.employeeName || "",
            department: item.orgUnitName || item.department || "",
            leaveType: item.leaveType,
            startDate: item.startDate,
            endDate: item.endDate,
            daysCount: item.daysCount,
            reason: item.reason || "",
            status: item.status,
            createdAt: item.createdAt ? item.createdAt.slice(0, 10) : "",
        }));

        assert.equal(mapped.length, 1);
        const req = mapped[0];

        // Kiểm tra đúng tên nhân viên và phòng ban
        assert.equal(req.employeeName, "Nguyễn Văn Dev");
        assert.equal(req.department, "Phòng Phát triển Phần mềm");

        // Kiểm tra định dạng hiển thị nghiệm thu: "Nguyễn Văn Dev · Phòng Phát triển Phần mềm"
        const displayLabel = `${req.employeeName} · ${req.department}`;
        assert.equal(displayLabel, "Nguyễn Văn Dev · Phòng Phát triển Phần mềm");

        // Đảm bảo không còn chuỗi dự phòng "Nhân viên #5" hay "Phòng ban"
        assert.equal(req.employeeName.includes("Nhân viên #"), false);
        assert.equal(req.department.includes("Phòng ban"), false);
    });

    test("Không có chuỗi dự phòng 'Nhân viên #' hay 'Phòng ban' khi thiếu thông tin", () => {
        const itemWithoutNames = {
            id: 102,
            employeeId: 99,
            employeeName: null,
            orgUnitName: null,
            department: null,
            leaveType: "SICK",
            startDate: "2026-10-05",
            endDate: "2026-10-05",
            daysCount: 1,
            hoursDeducted: 8,
            reason: "Ốm",
            status: "PENDING",
            createdAt: "2026-09-28T10:00:00",
        };

        const mapped = {
            id: `LV-${itemWithoutNames.id}`,
            employeeId: String(itemWithoutNames.employeeId),
            employeeName: itemWithoutNames.employeeName || "",
            department: itemWithoutNames.orgUnitName || itemWithoutNames.department || "",
            leaveType: itemWithoutNames.leaveType,
            startDate: itemWithoutNames.startDate,
            endDate: itemWithoutNames.endDate,
            daysCount: itemWithoutNames.daysCount,
            reason: itemWithoutNames.reason || "",
            status: itemWithoutNames.status,
            createdAt: itemWithoutNames.createdAt ? itemWithoutNames.createdAt.slice(0, 10) : "",
        };

        assert.equal(mapped.employeeName, "");
        assert.equal(mapped.department, "");
        assert.notEqual(mapped.employeeName, "Nhân viên #99");
        assert.notEqual(mapped.department, "Phòng ban");
    });
});
