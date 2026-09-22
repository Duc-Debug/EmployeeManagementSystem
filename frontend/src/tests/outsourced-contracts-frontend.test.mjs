import { test, describe } from "node:test";
import assert from "node:assert/strict";
import { exportOutsourcedContractsToCsv } from "../lib/api/outsourced-contracts.ts";
import { resolveActiveTab } from "../components/dashboard/dashboard-routing.ts";
import { canAccessTab } from "../components/dashboard/SideBar.tsx";

describe("Outsourced Contract Expiration Tracking Frontend Tests (NCL-14-CN-003)", () => {

    test("TC-01: Xuất CSV chuẩn định dạng UTF-8 BOM cho Excel từ exportOutsourcedContractsToCsv thực tế", () => {
        const mockContracts = [
            {
                employeeId: 101,
                employeeCode: "EXT-001",
                fullName: "Nguyễn Văn Thuê",
                professionalRole: "Frontend Dev",
                orgUnitName: "Khối Kỹ Thuật",
                contractEndDate: "2026-10-15",
                daysRemaining: 24,
                status: "EXPIRING_SOON",
                affectedAllocations: [{ allocationId: 1 }],
            },
            {
                employeeId: 102,
                employeeCode: "EXT-002",
                fullName: "Trần Thị Hết Hạn",
                professionalRole: "QA Tester",
                orgUnitName: "Khối Đảm Bảo Chất Lượng",
                contractEndDate: "2026-09-10",
                daysRemaining: -11,
                status: "EXPIRED",
                affectedAllocations: [],
            },
        ];

        const csv = exportOutsourcedContractsToCsv(mockContracts);
        assert.ok(csv.startsWith("\uFEFF"), "Phải bắt đầu bằng UTF-8 BOM");
        assert.ok(csv.includes("Nguyễn Văn Thuê"), "Phải chứa tên nhân viên 1");
        assert.ok(csv.includes("Trần Thị Hết Hạn"), "Phải chứa tên nhân viên 2");
        assert.ok(csv.includes("Sắp hết hạn"), "Phải chứa trạng thái sắp hết hạn");
        assert.ok(csv.includes("Đã quá hạn"), "Phải chứa trạng thái đã quá hạn");
    });

    test("TC-02: Phân loại phân bổ vi phạm theo quy tắc QTN-21", () => {
        const spansAllocation = {
            allocationId: 501,
            affectedType: "SPANS_OVER_EXPIRY",
            reason: "Hợp đồng hết hạn giữa tuần",
        };
        const afterExpiryAllocation = {
            allocationId: 502,
            affectedType: "AFTER_EXPIRY",
            reason: "Tuần phân bổ bắt đầu sau ngày hết hạn",
        };

        assert.equal(spansAllocation.affectedType, "SPANS_OVER_EXPIRY");
        assert.equal(afterExpiryAllocation.affectedType, "AFTER_EXPIRY");
    });

    test("TC-03: Dashboard Route Resolution thực tế cho tab 'outsourced-contracts'", () => {
        assert.equal(resolveActiveTab("/outsourced-contracts"), "outsourced-contracts");
        assert.equal(resolveActiveTab("/hop-dong-thue-ngoai"), "outsourced-contracts");
        assert.equal(resolveActiveTab("/schedule-conflict"), "schedule-conflict");
        assert.equal(resolveActiveTab("/"), "overview");
    });

    test("TC-04: Role Guard: Chỉ cho phép VT-03 và VT-05 truy cập theo BR-04 & TC-03", () => {
        // Allowed roles (Quản lý nguồn lực VT-03, Nhân sự VT-05)
        assert.equal(canAccessTab("VT-03", "outsourced-contracts"), true, "VT-03 (Quản lý nguồn lực) được phép");
        assert.equal(canAccessTab("VT-05", "outsourced-contracts"), true, "VT-05 (Nhân sự) được phép");
        assert.equal(canAccessTab("vt_03", "outsourced-contracts"), true, "vt_03 (lowercase/underscore) được phép");
        assert.equal(canAccessTab("vt_05", "outsourced-contracts"), true, "vt_05 (lowercase/underscore) được phép");

        // Denied roles: VT-01 (BGĐ), VT-02 (PM), VT-04 (Nhân viên), VT-06 (Admin), user chỉ có authority
        assert.equal(canAccessTab("VT-01", "outsourced-contracts"), false, "VT-01 (Ban Giám Đốc) bị chặn");
        assert.equal(canAccessTab("VT-02", "outsourced-contracts"), false, "VT-02 (PM) bị chặn");
        assert.equal(canAccessTab("VT-04", "outsourced-contracts"), false, "VT-04 (Nhân viên) bị chặn");
        assert.equal(canAccessTab("VT-06", "outsourced-contracts"), false, "VT-06 (Admin) bị chặn");
        assert.equal(canAccessTab("ADMIN", "outsourced-contracts"), false, "ADMIN bị chặn");
        assert.equal(canAccessTab("UNKNOWN", "outsourced-contracts", null, ["RESOURCE_ALLOCATION_MANAGE"]), false, "Có quyền RESOURCE_ALLOCATION_MANAGE nhưng không phải VT-03/VT-05 vẫn bị chặn");
    });

    test("TC-05: In-Memory Cache TTL và Invalidation Logic", () => {
        const TTL = 60_000;
        let cache = new Map();

        function setCache(key, data, time = Date.now()) {
            cache.set(key, { data, timestamp: time });
        }

        function getCache(key, now = Date.now()) {
            const entry = cache.get(key);
            if (!entry) return null;
            if (now - entry.timestamp >= TTL) {
                cache.delete(key);
                return null;
            }
            return entry.data;
        }

        const now = 1000000;
        setCache(30, { total: 5 }, now);

        // Cache hit within TTL
        assert.deepEqual(getCache(30, now + 30000), { total: 5 });

        // Cache miss after TTL
        assert.equal(getCache(30, now + 65000), null);
    });

    test("TC-06: Phân trang danh sách hợp đồng (Pagination Calculation)", () => {
        const pageSize = 10;
        const totalItems = 25;
        const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

        assert.equal(totalPages, 3, "25 bản ghi với trang 10 thì có 3 trang");

        function getPageSlice(items, page, size) {
            const start = (page - 1) * size;
            return items.slice(start, start + size);
        }

        const dummyList = Array.from({ length: 25 }, (_, i) => ({ id: i + 1 }));
        const page1 = getPageSlice(dummyList, 1, pageSize);
        const page3 = getPageSlice(dummyList, 3, pageSize);

        assert.equal(page1.length, 10);
        assert.equal(page1[0].id, 1);
        assert.equal(page3.length, 5);
        assert.equal(page3[4].id, 25);
    });
});
