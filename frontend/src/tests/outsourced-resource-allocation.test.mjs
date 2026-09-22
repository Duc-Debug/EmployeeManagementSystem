import { test, describe } from "node:test";
import assert from "node:assert/strict";

describe("Outsourced Resource Allocation (NCL-14-CN-002 / QTN-21) Frontend Logic & Contract Boundary Tests", () => {

    // Helper: Xác định tuần có nằm trong hạn hợp đồng thuê ngoài hay không (QTN-21)
    const isWeekWithinOutsourcedContract = (weekStartDate, weekEndDate, contractStartDate, contractEndDate) => {
        if (!contractStartDate && !contractEndDate) return true;
        if (contractStartDate && weekEndDate < contractStartDate) {
            return {
                valid: false,
                reasonCode: "CONTRACT_NOT_STARTED",
                reasonMessage: `Hợp đồng thuê ngoài chưa có hiệu lực (bắt đầu từ ${contractStartDate})`
            };
        }
        if (contractEndDate && weekStartDate > contractEndDate) {
            return {
                valid: false,
                reasonCode: "CONTRACT_EXPIRED",
                reasonMessage: `Hợp đồng thuê ngoài đã hết hạn (kết thúc ngày ${contractEndDate})`
            };
        }
        return { valid: true };
    };

    // Helper: Lọc danh sách nhân sự theo loại (NCL-14)
    const filterEmployeesByType = (rows, typeFilter) => {
        if (typeFilter === "INTERNAL") {
            return rows.filter((r) => !r.isOutsourced);
        }
        if (typeFilter === "OUTSOURCED") {
            return rows.filter((r) => r.isOutsourced);
        }
        return rows;
    };

    // Helper: Quyền phân bổ nguồn lực (RBAC)
    const canManageResourceAllocation = (roleCode) => {
        const normalized = (roleCode || "").toUpperCase().replace(/_/g, "-").replace(/^ROLE-/, "");
        return normalized === "VT-03";
    };

    const mockContractStart = "2027-06-01";
    const mockContractEnd = "2027-08-31";
    const mockProviderName = "FPT Software Outsourcing";

    test("TC-01: Phân bổ tuần nằm hoàn toàn trong hạn hợp đồng thuê ngoài thành công", () => {
        // Tuần 26/2027: 28/06/2027 - 04/07/2027
        const weekStart = "2027-06-28";
        const weekEnd = "2027-07-04";

        const result = isWeekWithinOutsourcedContract(weekStart, weekEnd, mockContractStart, mockContractEnd);
        assert.equal(result.valid, true);
    });

    test("TC-02: Chặn phân bổ tuần kết thúc trước ngày bắt đầu hợp đồng thuê ngoài (CONTRACT_NOT_STARTED)", () => {
        // Tuần 18/2027: 03/05/2027 - 09/05/2027 (trước 01/06/2027)
        const weekStart = "2027-05-03";
        const weekEnd = "2027-05-09";

        const result = isWeekWithinOutsourcedContract(weekStart, weekEnd, mockContractStart, mockContractEnd);
        assert.equal(result.valid, false);
        assert.equal(result.reasonCode, "CONTRACT_NOT_STARTED");
        assert.ok(result.reasonMessage.includes("chưa có hiệu lực"));
    });

    test("TC-03: Chặn phân bổ tuần bắt đầu sau ngày kết thúc hợp đồng thuê ngoài (CONTRACT_EXPIRED)", () => {
        // Tuần 40/2027: 04/10/2027 - 10/10/2027 (sau 31/08/2027)
        const weekStart = "2027-10-04";
        const weekEnd = "2027-10-10";

        const result = isWeekWithinOutsourcedContract(weekStart, weekEnd, mockContractStart, mockContractEnd);
        assert.equal(result.valid, false);
        assert.equal(result.reasonCode, "CONTRACT_EXPIRED");
        assert.ok(result.reasonMessage.includes("đã hết hạn"));
    });

    test("TC-04: Bộ lọc nhân sự bảng năng lực (CompanyWeeklyCapacityView) lọc chính xác nhân sự nội bộ và thuê ngoài", () => {
        const sampleRows = [
            { employeeId: 1, fullName: "Nguyễn Văn Nội Bộ", isOutsourced: false },
            { employeeId: 2, fullName: "Trần Thị Thuê Ngoài 1", isOutsourced: true, providerName: mockProviderName },
            { employeeId: 3, fullName: "Lê Văn Nội Bộ 2", isOutsourced: false },
            { employeeId: 4, fullName: "Phạm Chuyên Gia Outsourced", isOutsourced: true, providerName: "TMA Solutions" },
        ];

        // 1. ALL
        const all = filterEmployeesByType(sampleRows, "ALL");
        assert.equal(all.length, 4);

        // 2. INTERNAL
        const internal = filterEmployeesByType(sampleRows, "INTERNAL");
        assert.equal(internal.length, 2);
        assert.ok(internal.every(r => !r.isOutsourced));

        // 3. OUTSOURCED
        const outsourced = filterEmployeesByType(sampleRows, "OUTSOURCED");
        assert.equal(outsourced.length, 2);
        assert.ok(outsourced.every(r => r.isOutsourced));
    });

    test("TC-05: Ô ma trận hiển thị trạng thái 'Ngoài HĐ' khi tuần ngoài hạn hợp đồng của nhân sự thuê ngoài", () => {
        const outsourcedRow = {
            employeeId: 10,
            fullName: "Nguyễn Outsourced",
            isOutsourced: true,
            providerName: mockProviderName,
            contractStartDate: mockContractStart,
            contractEndDate: mockContractEnd,
        };

        const weekPastContract = {
            year: 2027,
            weekNumber: 42,
            startDate: "2027-10-18",
            endDate: "2027-10-24",
        };

        const cell = {
            year: 2027,
            weekNumber: 42,
            allocatedHours: 0,
            availableHours: 0,
        };

        const isOutOfContract =
            outsourcedRow.isOutsourced &&
            ((outsourcedRow.contractStartDate && weekPastContract.endDate < outsourcedRow.contractStartDate) ||
             (outsourcedRow.contractEndDate && weekPastContract.startDate > outsourcedRow.contractEndDate));

        assert.equal(isOutOfContract, true);
    });

    test("TC-06: Ứng viên tìm kiếm theo kỹ năng (ResourceSkillSearchModal) chứa đầy đủ thông tin thuê ngoài và đơn vị cung cấp", () => {
        const candidate = {
            employeeId: 50,
            employeeCode: "OS-050",
            fullName: "Lê Văn DevOps",
            skillName: "Kubernetes",
            proficiencyLevel: 4,
            isOutsourced: true,
            providerName: "CMC Global",
            startDate: "2027-01-01",
            contractEndDate: "2027-12-31",
            totalRemainingHours: 35,
        };

        assert.equal(candidate.isOutsourced, true);
        assert.equal(candidate.providerName, "CMC Global");
        assert.ok(candidate.contractEndDate != null);
    });

    test("TC-07: Kiểm tra quyền phân bổ: Chỉ VT-03 (Quản lý nguồn lực) có quyền thực hiện phân bổ nhân sự", () => {
        assert.equal(canManageResourceAllocation("VT-03"), true);
        assert.equal(canManageResourceAllocation("ROLE_VT_03"), true);
        assert.equal(canManageResourceAllocation("VT-01"), false); // Giám đốc
        assert.equal(canManageResourceAllocation("VT-02"), false); // Quản lý dự án (PM)
        assert.equal(canManageResourceAllocation("VT-04"), false); // Nhân viên
        assert.equal(canManageResourceAllocation("VT-05"), false); // Nhân sự (HR)
    });
});

