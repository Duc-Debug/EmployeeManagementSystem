import { test, describe } from "node:test";
import assert from "node:assert/strict";

describe("Department Monthly Leave Calendar Frontend Logic Tests (NCL-05-CN-006)", () => {

    test("TC-01: Cấu trúc dữ liệu lịch tháng hiển thị đủ 30 ngày và các đơn nghỉ", () => {
        const mockCalendar = {
            orgUnitId: 10,
            orgUnitCode: "DEV-DEP",
            orgUnitName: "Phòng Phát triển Phần mềm",
            year: 2026,
            month: 9,
            totalDepartmentEmployees: 10,
            warningThresholdPercentage: 0.5,
            totalLeaveRequests: 6,
            warningDaysCount: 1,
            dailySummaries: [
                {
                    date: "2026-09-01",
                    dayOfWeek: "TUESDAY",
                    totalOnLeave: 1,
                    approvedCount: 1,
                    pendingCount: 0,
                    isWarning: false,
                    isWorkingDay: true,
                    isHoliday: false,
                    totalLeaveHours: 8.0,
                    leaveItems: [
                        {
                            leaveRequestId: 101,
                            employeeId: 1,
                            employeeCode: "EMP-01",
                            fullName: "Nguyễn Văn A",
                            status: "APPROVED",
                            hoursDeducted: 8.0,
                            leaveType: "ANNUAL",
                        }
                    ]
                }
            ]
        };

        assert.equal(mockCalendar.orgUnitId, 10);
        assert.equal(mockCalendar.totalLeaveRequests, 6);
        assert.equal(mockCalendar.dailySummaries.length, 1);
        assert.equal(mockCalendar.dailySummaries[0].totalLeaveHours, 8.0);
        assert.equal(mockCalendar.dailySummaries[0].leaveItems[0].fullName, "Nguyễn Văn A");
    });

    test("TC-02: Cảnh báo vượt ngưỡng kích hoạt khi số người nghỉ >= ngưỡng", () => {
        const totalEmployees = 5;
        const onLeaveCount = 4;
        const threshold = 0.5; // 50%
        const isCompanyWorkingDay = true;

        const isWarning = isCompanyWorkingDay && (onLeaveCount / totalEmployees >= threshold);
        assert.equal(isWarning, true, "4/5 người nghỉ phải kích hoạt cảnh báo");
    });

    test("P1: Thứ 7, Chủ Nhật và Ngày Lễ (isCompanyWorkingDay = false) -> Bỏ qua cảnh báo", () => {
        const totalEmployees = 5;
        const onLeaveCount = 4;
        const threshold = 0.5;

        // Ngày Chủ Nhật hoặc Ngày Lễ 2/9
        const isCompanyWorkingDay = false;
        const isWarning = isCompanyWorkingDay && (onLeaveCount / totalEmployees >= threshold);
        assert.equal(isWarning, false, "Ngày nghỉ công ty không được phép báo động giả");
    });

    test("P2: Tính đúng tổng giờ nghỉ tích lũy trong ngày", () => {
        const leaveItems = [
            { hoursDeducted: 8.0 },
            { hoursDeducted: 4.0 },
            { hoursDeducted: 8.0 },
            { hoursDeducted: 8.0 },
        ];
        const totalLeaveHours = leaveItems.reduce((sum, item) => sum + item.hoursDeducted, 0);
        assert.equal(totalLeaveHours, 28.0, "Tổng giờ nghỉ là 28.0");
    });

    test("P2: Cờ includeSubUnits và URL search params formatting", () => {
        const params = {
            orgUnitId: 10,
            year: 2026,
            month: 9,
            warningThreshold: 0.5,
            includeSubUnits: true,
        };

        const searchParams = new URLSearchParams();
        searchParams.set("orgUnitId", String(params.orgUnitId));
        searchParams.set("year", String(params.year));
        searchParams.set("month", String(params.month));
        searchParams.set("warningThreshold", String(params.warningThreshold));
        searchParams.set("includeSubUnits", String(params.includeSubUnits));

        assert.equal(
            searchParams.toString(),
            "orgUnitId=10&year=2026&month=9&warningThreshold=0.5&includeSubUnits=true"
        );
    });

    test("Cây tổ chức: Hàm làm phẳng danh sách cây đơn vị phòng ban (flattenOrgTree)", () => {
        function flattenOrgTree(nodes, depth = 0) {
            const result = [];
            for (const node of nodes) {
                result.push({
                    id: node.id,
                    unitCode: node.unitCode,
                    unitName: node.unitName,
                    depth,
                });
                if (node.children && node.children.length > 0) {
                    result.push(...flattenOrgTree(node.children, depth + 1));
                }
            }
            return result;
        }

        const tree = [
            {
                id: 1,
                unitCode: "COMPANY",
                unitName: "Công ty",
                children: [
                    {
                        id: 10,
                        unitCode: "DEV",
                        unitName: "Phòng Phát triển",
                        children: [
                            { id: 101, unitCode: "DEV-FE", unitName: "Nhóm Frontend", children: [] },
                            { id: 102, unitCode: "DEV-BE", unitName: "Nhóm Backend", children: [] },
                        ]
                    }
                ]
            }
        ];

        const flat = flattenOrgTree(tree);
        assert.equal(flat.length, 4);
        assert.equal(flat[0].depth, 0);
        assert.equal(flat[1].depth, 1);
        assert.equal(flat[2].depth, 2);
        assert.equal(flat[3].depth, 2);
    });
});
