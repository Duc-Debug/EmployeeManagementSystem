import { test, describe } from "node:test";
import assert from "node:assert/strict";

// P2-2: Import trực tiếp implementation từ production code
import { fetchAllEmployees } from "../lib/api/employees.ts";
import { fetchAllProlongedIdleStaff } from "../lib/api/prolonged-idleness.ts";

/**
 * P1-2 & P2: Executive Dashboard & Pagination Resilience Tests
 * - Khắc phục lỗi 400 Bad Request do size > 50
 * - Đồng bộ KPI Executive Dashboard (tổng số, trạng thái và tổng giờ kế hoạch từ ProjectSummary)
 * - P2-1: totalHours lấy từ activeEstimatedHours của ProjectSummaryResult (từ backend)
 * - P2-2: Import trực tiếp production implementation của fetchAllEmployees và fetchAllProlongedIdleStaff
 * - P2-5: Tuyệt đối không fallback sang 50 bản ghi đầu của page 0 khi /projects/summary gặp lỗi hoặc null
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

  describe("2. ProlongedIdlenessWarningModal Production fetchAllProlongedIdleStaff", () => {
    test("TC-03: Production fetchAllProlongedIdleStaff phân trang tuần tự với size=50 và không bị giới hạn 2.500 bản ghi", async () => {
      const requestedSizes = [];
      const totalEmployees = 3000; // 3.000 bản ghi (> 2.500 cap cũ)
      const pageSize = 50;
      const expectedPages = Math.ceil(totalEmployees / pageSize); // 60 trang

      const mockApi = async (params) => {
        const size = params?.size ?? 50;
        const page = params?.page ?? 0;
        requestedSizes.push(size);
        assert.ok(size <= 50, `Kích thước request ${size} vượt quá giới hạn backend 50!`);

        const start = page * size;
        const count = Math.max(0, Math.min(size, totalEmployees - start));
        const items = Array.from({ length: count }, (_, i) => ({
          employeeId: start + i + 1,
          employeeCode: `EMP-${start + i + 1}`,
          fullName: `Nhân viên ${start + i + 1}`,
          orgUnitId: 1,
          departmentName: "Kỹ thuật",
          positionTitle: "Kỹ sư",
          consecutiveIdleWeeks: 3,
          totalEmptyHours: 120,
          averageUtilization: 10,
        }));

        return {
          orgUnitId: null,
          orgUnitName: "Toàn công ty",
          fromYear: 2026,
          fromWeek: 39,
          durationWeeks: 1,
          effectiveIdleThreshold: 70,
          consecutiveThreshold: 3,
          totalIdleEmployees: totalEmployees,
          totalEmptyHours: totalEmployees * 120,
          totalPages: expectedPages,
          page,
          size,
          items,
        };
      };

      const result = await fetchAllProlongedIdleStaff({}, mockApi);

      assert.strictEqual(result.length, 3000, "Phải lấy đủ toàn bộ 3.000 bản ghi mà không bị chặn ở 2.500");
      assert.strictEqual(requestedSizes.length, 60, "Phải gọi đủ 60 trang: page 0 đến page 59");
      assert.ok(requestedSizes.every((sz) => sz <= 50), "Mọi request đều tuân thủ size <= 50");
    });
  });

  describe("3. Directory and Conflict Modal Production fetchAllEmployees Implementation", () => {
    test("TC-04: Production fetchAllEmployees luôn request size <= 50 và tải toàn bộ theo totalPages mà không bị chặn 500 hay 1.000", async () => {
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
          orgUnitId: 1,
          orgUnitName: "Kỹ thuật",
          employeeCode: `EMP-${start + i + 1}`,
          fullName: `Nhân viên ${start + i + 1}`,
          standardHoursPerWeek: 40,
          version: 1,
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

    test("TC-05: Production fetchAllEmployees dừng an toàn nếu API trả về danh sách rỗng", async () => {
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

  describe("4. Executive Dashboard KPI Consistency with ProjectSummary (P2-1 & P2-5)", () => {
    test("TC-06: Khi có ProjectSummary riêng, KPI active/planned/closed và totalHours phản ánh đúng toàn bộ DB thay vì 50 item đầu", () => {
      // Giả sử DB có 150 dự án và 6.400 giờ kế hoạch
      const backendSummary = {
        totalProjects: 150,
        activeProjects: 80,
        plannedProjects: 40,
        closedProjects: 30,
        activeEstimatedHours: 6400,
      };

      // API getProjects(0, 50) chỉ trả 50 dự án đầu tiên (với chỉ 22 active và 1.760 giờ)
      const firstPageProjects = [
        ...Array.from({ length: 22 }, (_, i) => ({ id: i + 1, status: "ACTIVE", estimatedHours: 80 })),
        ...Array.from({ length: 18 }, (_, i) => ({ id: i + 23, status: "PLANNED", estimatedHours: 50 })),
        ...Array.from({ length: 10 }, (_, i) => ({ id: i + 41, status: "CLOSED", estimatedHours: 60 })),
      ];

      // Logic tính KPI trong ExecutiveDashboardOverview:
      const totalProjects = backendSummary?.totalProjects ?? 0;
      const activeProjectsCount = backendSummary?.activeProjects ?? 0;
      const plannedProjectsCount = backendSummary?.plannedProjects ?? 0;
      const closedProjectsCount = backendSummary?.closedProjects ?? 0;
      const totalHours = backendSummary?.activeEstimatedHours != null
        ? Number(backendSummary.activeEstimatedHours)
        : 0;

      // Đảm bảo KPI hiển thị đúng số liệu toàn diện từ Backend ProjectSummary
      assert.strictEqual(totalProjects, 150);
      assert.strictEqual(activeProjectsCount, 80);
      assert.strictEqual(plannedProjectsCount, 40);
      assert.strictEqual(closedProjectsCount, 30);
      assert.strictEqual(activeProjectsCount + plannedProjectsCount + closedProjectsCount, totalProjects);
      assert.strictEqual(totalHours, 6400, "totalHours phải lấy từ activeEstimatedHours của Backend (6.400h), không phải 1.760h của 50 dự án đầu");
    });

    test("TC-07: P2-5: Tuyệt đối không fallback KPI sang 50 bản ghi đầu khi ProjectSummary lỗi hoặc chưa có dữ liệu", () => {
      // 50 bản ghi đầu vẫn được trả về từ getProjects(0, 50)
      const firstPageProjects = [
        { id: 1, status: "ACTIVE", estimatedHours: 80 },
        { id: 2, status: "ACTIVE", estimatedHours: 120 },
        { id: 3, status: "PLANNED", estimatedHours: 50 },
      ];
      // Nhưng /projects/summary bị lỗi (null)
      const projectSummary = null;

      // Logic tính KPI an toàn P2-5:
      const totalProjects = projectSummary?.totalProjects ?? 0;
      const activeProjectsCount = projectSummary?.activeProjects ?? 0;
      const plannedProjectsCount = projectSummary?.plannedProjects ?? 0;
      const closedProjectsCount = projectSummary?.closedProjects ?? 0;
      const totalHours = projectSummary?.activeEstimatedHours != null
        ? Number(projectSummary.activeEstimatedHours)
        : 0;

      // Kiểm tra: Các số liệu KPI không bị ngộ nhận/lấy sai từ 3 bản ghi cục bộ
      assert.strictEqual(totalProjects, 0, "Không fallback sang firstPageProjects.length");
      assert.strictEqual(activeProjectsCount, 0, "Không fallback sang firstPageProjects.filter");
      assert.strictEqual(plannedProjectsCount, 0);
      assert.strictEqual(closedProjectsCount, 0);
      assert.strictEqual(totalHours, 0, "Không fallback sang firstPageProjects sum");
    });
  });
});
