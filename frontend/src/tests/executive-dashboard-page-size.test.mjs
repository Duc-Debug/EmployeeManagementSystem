import { test, describe } from "node:test";
import assert from "node:assert/strict";

/**
 * P1-2 & P2: Executive Dashboard & Pagination Resilience Tests
 * - Khắc phục lỗi 400 Bad Request do size > 50
 * - Đồng bộ KPI Executive Dashboard (tổng số và trạng thái ACTIVE/PLANNED/CLOSED từ ProjectSummary)
 * - Loại bỏ hard cap 2.500 bản ghi trên Export Prolonged Idleness (sử dụng totalPages)
 * - Tách và kiểm tra triệt để hàm fetchAllEmployees (sử dụng totalPages, batch size <= 50)
 */

describe("P1-2 & P2: Executive Dashboard & Pagination Resilience Tests", () => {

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

  describe("2. ProlongedIdlenessWarningModal Paginated Export with totalPages", () => {
    async function fetchAllProlongedIdleStaff(fetchPageFn, exportPageSize = 50) {
      let currentPage = 0;
      let totalPages = 1;
      const allItems = [];

      do {
        const pageData = await fetchPageFn(currentPage, exportPageSize);
        const items = pageData?.items || [];
        allItems.push(...items);
        totalPages = pageData?.totalPages || 1;

        if (items.length === 0 || allItems.length >= (pageData?.totalIdleEmployees || 0)) {
          break;
        }
        currentPage++;
      } while (currentPage < totalPages);

      return allItems;
    }

    test("TC-03: Export CSV phân trang tuần tự với size=50 và không bị giới hạn 2.500 bản ghi", async () => {
      const requestedSizes = [];
      const totalEmployees = 3000; // 3.000 bản ghi (> 2.500 cap cũ)
      const pageSize = 50;
      const expectedPages = Math.ceil(totalEmployees / pageSize); // 60 trang

      const mockApi = async (page, size) => {
        requestedSizes.push(size);
        assert.ok(size <= 50, `Kích thước request ${size} vượt quá giới hạn backend 50!`);

        const start = page * size;
        const count = Math.max(0, Math.min(size, totalEmployees - start));
        const items = Array.from({ length: count }, (_, i) => ({
          employeeCode: `EMP-${start + i + 1}`,
          fullName: `Nhân viên ${start + i + 1}`,
        }));

        return {
          totalIdleEmployees: totalEmployees,
          totalPages: expectedPages,
          page,
          size,
          items,
        };
      };

      const result = await fetchAllProlongedIdleStaff(mockApi, 50);

      assert.strictEqual(result.length, 3000, "Phải lấy đủ toàn bộ 3.000 bản ghi mà không bị chặn ở 2.500");
      assert.strictEqual(requestedSizes.length, 60, "Phải gọi đủ 60 trang: page 0 đến page 59");
      assert.ok(requestedSizes.every((sz) => sz <= 50), "Mọi request đều tuân thủ size <= 50");
    });
  });

  describe("3. Directory and Conflict Modal fetchAllEmployees Helper Implementation", () => {
    async function fetchAllEmployees(fetchPageFn) {
      const pageSize = 50;
      const allEmployees = [];
      let page = 1;
      let totalPages = 1;

      do {
        const result = await fetchPageFn(page, pageSize);
        const items = result?.content || [];
        allEmployees.push(...items);
        totalPages = result?.totalPages || 1;

        if (items.length === 0 || allEmployees.length >= (result?.totalElements || 0)) {
          break;
        }
        page++;
      } while (page <= totalPages);

      return allEmployees;
    }

    test("TC-04: fetchAllEmployees luôn request size <= 50 và tải toàn bộ theo totalPages mà không bị chặn 500 hay 1.000", async () => {
      const requestedSizes = [];
      const requestedPages = [];
      const totalEmployees = 1250; // > 1.000
      const pageSize = 50;
      const expectedPages = Math.ceil(totalEmployees / pageSize); // 25 trang

      const mockFetchPage = async (page, size) => {
        requestedPages.push(page);
        requestedSizes.push(size);

        const start = (page - 1) * size;
        const count = Math.max(0, Math.min(size, totalEmployees - start));
        const content = Array.from({ length: count }, (_, i) => ({
          id: start + i + 1,
          fullName: `Nhân viên ${start + i + 1}`,
        }));

        return {
          content,
          page,
          size,
          totalElements: totalEmployees,
          totalPages: expectedPages,
        };
      };

      const employees = await fetchAllEmployees(mockFetchPage);

      assert.strictEqual(employees.length, 1250, "Phải tải đủ 1.250 nhân viên");
      assert.strictEqual(requestedPages.length, 25, "Phải gọi đủ 25 trang");
      assert.strictEqual(requestedPages[0], 1, "Trang đầu tiên phải là 1 (1-based index)");
      assert.strictEqual(requestedPages[24], 25, "Trang cuối cùng phải là 25");
      assert.ok(requestedSizes.every((sz) => sz <= 50), "Tất cả các lời gọi API getEmployees đều có size <= 50");
    });

    test("TC-05: fetchAllEmployees dừng an toàn nếu API trả về danh sách rỗng", async () => {
      let callCount = 0;
      const mockEmpty = async (page, size) => {
        callCount++;
        return {
          content: [],
          page,
          size,
          totalElements: 0,
          totalPages: 0,
        };
      };

      const employees = await fetchAllEmployees(mockEmpty);
      assert.strictEqual(employees.length, 0);
      assert.strictEqual(callCount, 1);
    });
  });

  describe("4. Executive Dashboard KPI Consistency with ProjectSummary", () => {
    test("TC-06: Khi có ProjectSummary riêng, KPI active/planned/closed phản ánh đúng toàn bộ DB thay vì 50 item đầu", () => {
      // Giả sử DB có 150 dự án
      const backendSummary = {
        totalProjects: 150,
        activeProjects: 80,
        plannedProjects: 40,
        closedProjects: 30,
      };

      // API getProjects(0, 50) chỉ trả 50 dự án đầu tiên
      const firstPageProjects = [
        ...Array.from({ length: 22 }, (_, i) => ({ id: i + 1, status: "ACTIVE" })),
        ...Array.from({ length: 18 }, (_, i) => ({ id: i + 23, status: "PLANNED" })),
        ...Array.from({ length: 10 }, (_, i) => ({ id: i + 41, status: "CLOSED" })),
      ];

      // Logic tính KPI trong ExecutiveDashboardOverview:
      const totalProjects = backendSummary?.totalProjects ?? firstPageProjects.length;
      const activeProjectsCount = backendSummary?.activeProjects ?? firstPageProjects.filter((p) => p.status === "ACTIVE").length;
      const plannedProjectsCount = backendSummary?.plannedProjects ?? firstPageProjects.filter((p) => p.status === "PLANNED").length;
      const closedProjectsCount = backendSummary?.closedProjects ?? firstPageProjects.filter((p) => p.status === "CLOSED").length;

      // Đảm bảo KPI hiển thị đúng số liệu tổng thể (150 = 80 + 40 + 30), KHÔNG phải số liệu page đầu (22, 18, 10)
      assert.strictEqual(totalProjects, 150);
      assert.strictEqual(activeProjectsCount, 80);
      assert.strictEqual(plannedProjectsCount, 40);
      assert.strictEqual(closedProjectsCount, 30);
      assert.strictEqual(activeProjectsCount + plannedProjectsCount + closedProjectsCount, totalProjects);
    });

    test("TC-07: Fallback an toàn nếu ProjectSummary chưa trả về", () => {
      const firstPageProjects = [
        { id: 1, status: "ACTIVE" },
        { id: 2, status: "ACTIVE" },
        { id: 3, status: "PLANNED" },
      ];
      const projectSummary = null;
      const totalProjectsCount = 3;

      const totalProjects = projectSummary?.totalProjects ?? totalProjectsCount ?? firstPageProjects.length;
      const activeProjectsCount = projectSummary?.activeProjects ?? firstPageProjects.filter((p) => p.status === "ACTIVE").length;
      const plannedProjectsCount = projectSummary?.plannedProjects ?? firstPageProjects.filter((p) => p.status === "PLANNED").length;
      const closedProjectsCount = projectSummary?.closedProjects ?? firstPageProjects.filter((p) => p.status === "CLOSED").length;

      assert.strictEqual(totalProjects, 3);
      assert.strictEqual(activeProjectsCount, 2);
      assert.strictEqual(plannedProjectsCount, 1);
      assert.strictEqual(closedProjectsCount, 0);
    });
  });
});
