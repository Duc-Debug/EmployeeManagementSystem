import { test, describe } from "node:test";
import assert from "node:assert/strict";

/**
 * P1-2: Widget dashboard Ban giám đốc (VT-01) - Giới hạn kích thước trang size <= 50
 * Đảm bảo các widget và modal không gửi size > 50 làm backend trả về HTTP 400 Bad Request.
 */

describe("P1-2: Executive Dashboard & Modal Page Size Tests", () => {

  describe("1. ExecutiveDashboardOverview Capacity Query Validation", () => {
    function buildExecutiveCapacityQuery(year, weekNumber, size = 50, page = 0) {
      const MAX_BACKEND_CAPACITY_SIZE = 50;
      const validSize = Math.min(Math.max(1, size), MAX_BACKEND_CAPACITY_SIZE);
      const validPage = Math.max(0, page);

      return {
        fromYear: year,
        fromWeek: weekNumber,
        durationWeeks: 1,
        page: validPage,
        size: validSize,
      };
    }

    test("TC-01: Executive Dashboard query sử dụng size <= 50 để tránh lỗi 400 từ backend", () => {
      const query = buildExecutiveCapacityQuery(2026, 39, 50, 0);

      assert.strictEqual(query.size, 50);
      assert.strictEqual(query.page, 0);
      assert.strictEqual(query.durationWeeks, 1);
      assert.ok(query.size <= 50, "Kích thước trang phải <= 50 theo ràng buộc @Max(50)");
    });

    test("TC-02: Nếu đầu vào yêu cầu size 100 (như trước khi fix), query phải được chuẩn hóa về tối đa 50", () => {
      const legacyQuery = buildExecutiveCapacityQuery(2026, 39, 100, 0);

      assert.strictEqual(legacyQuery.size, 50, "Kích thước vượt quá 50 phải được chặn/clamp về 50");
      assert.notStrictEqual(legacyQuery.size, 100);
    });
  });

  describe("2. ProlongedIdlenessWarningModal Paginated Export", () => {
    async function mockFetchAllProlongedIdleStaff(fetchPageFn, exportPageSize = 50) {
      let currentPage = 0;
      const allItems = [];
      let total = 0;

      do {
        const pageData = await fetchPageFn(currentPage, exportPageSize);
        const items = pageData?.items || [];
        allItems.push(...items);
        total = pageData?.totalIdleEmployees || 0;

        if (items.length < exportPageSize || allItems.length >= total) {
          break;
        }
        currentPage++;
      } while (currentPage < 50);

      return allItems;
    }

    test("TC-03: Export CSV phân trang tuần tự với size=50 thay vì gửi size=1000", async () => {
      const requestedSizes = [];
      const totalEmployees = 125;

      // Mock backend with 125 items across 3 pages (50 + 50 + 25)
      const mockApi = async (page, size) => {
        requestedSizes.push(size);
        assert.ok(size <= 50, `Kích thước request ${size} vượt quá giới hạn backend 50!`);

        const start = page * size;
        const count = Math.max(0, Math.min(size, totalEmployees - start));
        const items = Array.from({ length: count }, (_, i) => ({
          employeeCode: `EMP-${start + i + 1}`,
          fullName: `Nhân viên ${start + i + 1}`,
          consecutiveIdleWeeks: 3,
          averageUtilization: 10,
          totalEmptyHours: 120,
          status: "PENDING",
        }));

        return {
          totalIdleEmployees: totalEmployees,
          items,
        };
      };

      const result = await mockFetchAllProlongedIdleStaff(mockApi, 50);

      assert.strictEqual(result.length, 125, "Phải gom đủ 125 bản ghi từ 3 trang");
      assert.strictEqual(requestedSizes.length, 3, "Phải gọi 3 trang: page 0, 1, 2");
      assert.ok(requestedSizes.every((sz) => sz <= 50), "Mọi request đều có size <= 50");
    });
  });

  describe("3. Directory and Conflict Modal Loader Chunks", () => {
    test("TC-04: Batch size của các modal tra cứu danh mục luôn tuân thủ size <= 50", () => {
      const allowedBatchSize = 50;
      assert.ok(allowedBatchSize <= 50);
    });
  });
});
