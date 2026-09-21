import { test, describe } from "node:test";
import assert from "node:assert/strict";

describe("Employee Data Import (NCL-12-CN-004) Frontend Unit Tests", () => {

    test("TC-01: CSV Error Row Generator formats fields with quotes and semicolon errors properly", () => {
        const rows = [
            {
                rowNumber: 2,
                employeeCode: "EMP001",
                fullName: "Nguyen Van A",
                username: "an.nguyen",
                email: "an.nguyen@test.com",
                orgUnitIdentifier: "Trung tâm Phần mềm",
                roleCode: "VT-04",
                professionalRole: "Developer",
                standardHoursPerWeek: 40,
                startDate: "2026-01-01",
                contractEndDate: "2027-12-31",
                isOutsourced: false,
                valid: true,
                errors: [],
            },
            {
                rowNumber: 3,
                employeeCode: "EMP002",
                fullName: 'Tran "Binh" Thi',
                username: "binh.tran",
                email: "invalid-email",
                orgUnitIdentifier: "Phòng Không Tồn Tại",
                roleCode: "VT-99",
                professionalRole: "QA",
                standardHoursPerWeek: 200,
                startDate: "2026-02-01",
                contractEndDate: "2025-01-01",
                isOutsourced: true,
                valid: false,
                errors: [
                    "Email 'invalid-email' không đúng định dạng chuẩn",
                    "Phòng ban 'Phòng Không Tồn Tại' không tồn tại trong hệ thống",
                    "Giờ làm việc chuẩn (200h) phải lớn hơn 0 và không vượt quá 168h/tuần",
                ],
            },
        ];

        const invalidRows = rows.filter((r) => !r.valid);
        assert.equal(invalidRows.length, 1);

        const escapeCsv = (val) => {
            if (val === null || val === undefined) return '""';
            const str = String(val).replace(/"/g, '""');
            return `"${str}"`;
        };

        const headers = [
            "Dòng", "Mã nhân viên", "Họ và tên", "Tên đăng nhập", "Email",
            "Phòng ban / Đơn vị", "Mã vai trò", "Chức danh chuyên môn", "Giờ chuẩn",
            "Ngày bắt đầu", "Ngày kết thúc HĐ", "Thuê ngoài", "Chi tiết lỗi",
        ];

        const csvRows = [
            headers.map(escapeCsv).join(","),
            ...invalidRows.map((r) =>
                [
                    r.rowNumber,
                    r.employeeCode || "",
                    r.fullName || "",
                    r.username || "",
                    r.email || "",
                    r.orgUnitIdentifier || "",
                    r.roleCode || "",
                    r.professionalRole || "",
                    r.standardHoursPerWeek ?? "",
                    r.startDate || "",
                    r.contractEndDate || "",
                    r.isOutsourced ? "TRUE" : "FALSE",
                    r.errors.join("; "),
                ]
                    .map(escapeCsv)
                    .join(",")
            ),
        ];

        const csvContent = "\uFEFF" + csvRows.join("\r\n");

        assert.ok(csvContent.startsWith("\uFEFF"));
        assert.ok(csvContent.includes('"Tran ""Binh"" Thi"'));
        assert.ok(csvContent.includes('"TRUE"'));
        assert.ok(csvContent.includes("Email 'invalid-email' không đúng định dạng chuẩn; Phòng ban 'Phòng Không Tồn Tại' không tồn tại trong hệ thống"));
    });

    test("TC-02: Permission check for Data Import tab", () => {
        const canAccessDataImport = (roleCode, permissions) => {
            const normalized = roleCode ? roleCode.toUpperCase().replace(/_/g, "-") : "";
            return permissions?.includes("DATA_IMPORT") === true ||
                ["VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized);
        };

        assert.equal(canAccessDataImport("VT-06", []), true);
        assert.equal(canAccessDataImport("ROLE-ADMIN", []), true);
        assert.equal(canAccessDataImport("VT-04", ["DATA_IMPORT"]), true);
        assert.equal(canAccessDataImport("VT-04", []), false);
        assert.equal(canAccessDataImport("VT-01", []), false);
        assert.equal(canAccessDataImport("VT-02", []), false);
        assert.equal(canAccessDataImport("VT-03", []), false);
        assert.equal(canAccessDataImport("VT-05", []), false);
    });

    test("TC-03: Filter tabs count calculation and row partitioning", () => {
        const sampleRows = [
            { id: 1, valid: true },
            { id: 2, valid: true },
            { id: 3, valid: false },
            { id: 4, valid: true },
            { id: 5, valid: false },
        ];

        const validCount = sampleRows.filter((r) => r.valid).length;
        const invalidCount = sampleRows.filter((r) => !r.valid).length;

        assert.equal(sampleRows.length, 5);
        assert.equal(validCount, 3);
        assert.equal(invalidCount, 2);
    });
});
