import { test, describe } from "node:test";
import assert from "node:assert/strict";

describe("Outsourced Employee Declaration (NCL-14-CN-001) Frontend Logic & Validation Tests", () => {

    const validateOutsourcedForm = (data) => {
        if (!data.fullName || !data.fullName.trim()) {
            return "Vui lòng nhập họ và tên nhân sự.";
        }
        if (!data.providerName || !data.providerName.trim()) {
            return "Vui lòng nhập tên đơn vị cung cấp.";
        }
        if (!data.orgUnitId) {
            return "Vui lòng chọn đơn vị phòng ban tiếp nhận.";
        }
        if (!data.startDate) {
            return "Vui lòng chọn ngày bắt đầu hợp đồng thuê.";
        }
        if (!data.contractEndDate) {
            return "Vui lòng chọn ngày kết thúc hợp đồng thuê.";
        }
        if (data.contractEndDate < data.startDate) {
            return "Ngày kết thúc hợp đồng thuê không được trước ngày bắt đầu.";
        }
        if (!data.standardHoursPerWeek || data.standardHoursPerWeek < 1 || data.standardHoursPerWeek > 168) {
            return "Số giờ chuẩn làm việc mỗi tuần phải từ 1 đến 168 giờ.";
        }
        return null;
    };

    const canDeclareOutsourced = (roleCode) => {
        const normalized = (roleCode || "").toUpperCase().replace(/_/g, "-");
        return normalized === "VT-05";
    };

    test("TC-01: Luồng thành công - Khai báo chuyên gia thuê ngoài 3 tháng hợp lệ", () => {
        const formData = {
            fullName: "Nguyễn Văn Chuyên Gia",
            providerName: "FPT Software",
            employeeCode: "EXT-001",
            orgUnitId: 2,
            professionalRole: "Senior Java Developer",
            startDate: "2026-10-01",
            contractEndDate: "2026-12-31",
            standardHoursPerWeek: 40,
            skillIds: [10, 15],
        };

        const error = validateOutsourcedForm(formData);
        assert.equal(error, null);

        // Verify payload format
        const payload = {
            fullName: formData.fullName.trim(),
            providerName: formData.providerName.trim(),
            employeeCode: formData.employeeCode?.trim() || undefined,
            orgUnitId: Number(formData.orgUnitId),
            professionalRole: formData.professionalRole?.trim() || undefined,
            startDate: formData.startDate,
            contractEndDate: formData.contractEndDate,
            standardHoursPerWeek: Number(formData.standardHoursPerWeek) || 40,
            skillIds: formData.skillIds?.length ? formData.skillIds : undefined,
        };

        assert.equal(payload.fullName, "Nguyễn Văn Chuyên Gia");
        assert.equal(payload.providerName, "FPT Software");
        assert.equal(payload.orgUnitId, 2);
        assert.deepEqual(payload.skillIds, [10, 15]);
    });

    test("TC-02: Ngoại lệ - contractEndDate trước startDate bị từ chối với thông báo lỗi", () => {
        const invalidDatesForm = {
            fullName: "Nguyễn Văn B",
            providerName: "TMA Solutions",
            orgUnitId: 1,
            startDate: "2026-10-15",
            contractEndDate: "2026-10-01",
            standardHoursPerWeek: 40,
        };

        const error = validateOutsourcedForm(invalidDatesForm);
        assert.equal(error, "Ngày kết thúc hợp đồng thuê không được trước ngày bắt đầu.");
    });

    test("TC-02b: Ngoại lệ - Ngày kết thúc bằng ngày bắt đầu là hợp lệ (hợp đồng 1 ngày)", () => {
        const sameDateForm = {
            fullName: "Nguyễn Văn C",
            providerName: "CMC Global",
            orgUnitId: 1,
            startDate: "2026-10-15",
            contractEndDate: "2026-10-15",
            standardHoursPerWeek: 8,
        };

        const error = validateOutsourcedForm(sameDateForm);
        assert.equal(error, null);
    });

    test("TC-03: Phân quyền RBAC - Chỉ VT-05 (Nhân sự) có quyền khai báo nhân sự thuê ngoài", () => {
        assert.equal(canDeclareOutsourced("VT-05"), true);
        assert.equal(canDeclareOutsourced("vt_05"), true);

        // Non-HR roles must be blocked
        assert.equal(canDeclareOutsourced("VT-01"), false); // Ban Giám Đốc
        assert.equal(canDeclareOutsourced("VT-02"), false); // Quản lý dự án (PM)
        assert.equal(canDeclareOutsourced("VT-03"), false); // Quản lý nguồn lực (RM)
        assert.equal(canDeclareOutsourced("VT-04"), false); // Nhân viên chuyên môn
        assert.equal(canDeclareOutsourced("VT-06"), false); // Quản trị hệ thống (Admin)
        assert.equal(canDeclareOutsourced("ROLE_ADMIN"), false);
        assert.equal(canDeclareOutsourced(""), false);
        assert.equal(canDeclareOutsourced(null), false);
    });

    test("TC-04: Kiểm tra tính hợp lệ của các trường bắt buộc (Required fields)", () => {
        assert.equal(
            validateOutsourcedForm({ fullName: "", providerName: "FPT", orgUnitId: 1, startDate: "2026-10-01", contractEndDate: "2026-12-31", standardHoursPerWeek: 40 }),
            "Vui lòng nhập họ và tên nhân sự."
        );
        assert.equal(
            validateOutsourcedForm({ fullName: "Chuyên Gia", providerName: "   ", orgUnitId: 1, startDate: "2026-10-01", contractEndDate: "2026-12-31", standardHoursPerWeek: 40 }),
            "Vui lòng nhập tên đơn vị cung cấp."
        );
        assert.equal(
            validateOutsourcedForm({ fullName: "Chuyên Gia", providerName: "FPT", orgUnitId: 0, startDate: "2026-10-01", contractEndDate: "2026-12-31", standardHoursPerWeek: 40 }),
            "Vui lòng chọn đơn vị phòng ban tiếp nhận."
        );
        assert.equal(
            validateOutsourcedForm({ fullName: "Chuyên Gia", providerName: "FPT", orgUnitId: 1, startDate: "", contractEndDate: "2026-12-31", standardHoursPerWeek: 40 }),
            "Vui lòng chọn ngày bắt đầu hợp đồng thuê."
        );
        assert.equal(
            validateOutsourcedForm({ fullName: "Chuyên Gia", providerName: "FPT", orgUnitId: 1, startDate: "2026-10-01", contractEndDate: "", standardHoursPerWeek: 40 }),
            "Vui lòng chọn ngày kết thúc hợp đồng thuê."
        );
    });

    test("TC-05: Kiểm tra biên số giờ làm việc chuẩn (standardHoursPerWeek: 1 - 168)", () => {
        const base = {
            fullName: "Nguyễn Văn D",
            providerName: "FPT Software",
            orgUnitId: 1,
            startDate: "2026-10-01",
            contractEndDate: "2026-12-31",
        };

        assert.equal(validateOutsourcedForm({ ...base, standardHoursPerWeek: 0 }), "Số giờ chuẩn làm việc mỗi tuần phải từ 1 đến 168 giờ.");
        assert.equal(validateOutsourcedForm({ ...base, standardHoursPerWeek: -5 }), "Số giờ chuẩn làm việc mỗi tuần phải từ 1 đến 168 giờ.");
        assert.equal(validateOutsourcedForm({ ...base, standardHoursPerWeek: 169 }), "Số giờ chuẩn làm việc mỗi tuần phải từ 1 đến 168 giờ.");
        assert.equal(validateOutsourcedForm({ ...base, standardHoursPerWeek: 1 }), null);
        assert.equal(validateOutsourcedForm({ ...base, standardHoursPerWeek: 168 }), null);
    });

    test("TC-06: Bộ lọc tìm kiếm trên giao diện hỗ trợ tìm kiếm theo Đơn vị cung cấp (providerName)", () => {
        const profiles = [
            { id: "1", fullName: "Lê Văn An", employeeCode: "NV001", department: "Khối Công nghệ", providerName: "", isOutsourced: false },
            { id: "2", fullName: "Trần Văn Bình", employeeCode: "EXT-001", department: "Phòng Phần mềm 1", providerName: "FPT Software", isOutsourced: true },
            { id: "3", fullName: "Nguyễn Thị Chi", employeeCode: "EXT-002", department: "Phòng Phần mềm 2", providerName: "TMA Solutions", isOutsourced: true },
        ];

        const search = (term) => {
            const q = term.trim().toLowerCase();
            return profiles.filter((p) =>
                p.fullName.toLowerCase().includes(q) ||
                p.employeeCode.toLowerCase().includes(q) ||
                p.department.toLowerCase().includes(q) ||
                (p.providerName && p.providerName.toLowerCase().includes(q))
            );
        };

        // Search by provider
        const fptResults = search("FPT");
        assert.equal(fptResults.length, 1);
        assert.equal(fptResults[0].employeeCode, "EXT-001");

        const tmaResults = search("TMA Solutions");
        assert.equal(tmaResults.length, 1);
        assert.equal(tmaResults[0].employeeCode, "EXT-002");

        // Search by employee code
        const codeResults = search("EXT");
        assert.equal(codeResults.length, 2);

        // Search by internal person
        const internalResults = search("Lê Văn An");
        assert.equal(internalResults.length, 1);
        assert.equal(internalResults[0].isOutsourced, false);
    });
});
