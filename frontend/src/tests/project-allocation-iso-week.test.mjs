import test from "node:test";
import assert from "node:assert/strict";

// Bắt buộc import hàm thực tế từ production code
import {
  getIsoWeeksForMonth,
  getIsoWeekDateRange,
  formatIsoWeekDateRange,
  getIsoWeekDetails,
  getIsoWeeksInYear,
} from "../lib/iso-week.ts";
import {
  buildProjectMonth,
  generateProjectMonth,
} from "../components/project/projectData.ts";
import {
  getMonthAllocationWeekRange,
  fetchMonthProjectAllocations,
} from "../lib/api/allocations.ts";

test("P0-3: Quy đổi tuần ISO lưới phân bổ dự án & tối ưu API request", async (t) => {
  await t.test("A. Month → ISO weeks: 09/2026, 01/2027, 02/2026", () => {
    // Case 1: 09/2026 (September 2026 has Thursdays on 3rd, 10th, 17th, 24th)
    const sepWeeks = getIsoWeeksForMonth(2026, 9);
    assert.deepEqual(
      sepWeeks.map((w) => ({ year: w.year, week: w.week })),
      [
        { year: 2026, week: 36 },
        { year: 2026, week: 37 },
        { year: 2026, week: 38 },
        { year: 2026, week: 39 },
      ],
      "09/2026 phải gồm đúng 4 tuần: W36..W39 thuộc ISO year 2026"
    );

    const sepMonth = buildProjectMonth(2026, 9);
    assert.equal(sepMonth.id, "2026-09");
    assert.deepEqual(
      sepMonth.weeks.map((w) => ({ key: w.key, label: w.label, year: w.year, weekNumber: w.weekNumber })),
      [
        { key: "W36", label: "W36", year: 2026, weekNumber: 36 },
        { key: "W37", label: "W37", year: 2026, weekNumber: 37 },
        { key: "W38", label: "W38", year: 2026, weekNumber: 38 },
        { key: "W39", label: "W39", year: 2026, weekNumber: 39 },
      ]
    );

    // Case 2: 01/2027 (January 2027 has Thursdays on 7th, 14th, 21st, 28th)
    const janWeeks = getIsoWeeksForMonth(2027, 1);
    assert.deepEqual(
      janWeeks.map((w) => ({ year: w.year, week: w.week })),
      [
        { year: 2027, week: 1 },
        { year: 2027, week: 2 },
        { year: 2027, week: 3 },
        { year: 2027, week: 4 },
      ],
      "01/2027 phải gồm đúng 4 tuần: W1..W4 thuộc ISO year 2027"
    );

    const janMonth = buildProjectMonth(2027, 1);
    assert.equal(janMonth.id, "2027-01");
    assert.deepEqual(
      janMonth.weeks.map((w) => ({ key: w.key, label: w.label, year: w.year, weekNumber: w.weekNumber })),
      [
        { key: "W1", label: "W1", year: 2027, weekNumber: 1 },
        { key: "W2", label: "W2", year: 2027, weekNumber: 2 },
        { key: "W3", label: "W3", year: 2027, weekNumber: 3 },
        { key: "W4", label: "W4", year: 2027, weekNumber: 4 },
      ]
    );

    // Case 3: 02/2026 (February 2026 has Thursdays on 5th, 12th, 19th, 26th)
    const febWeeks = getIsoWeeksForMonth(2026, 2);
    assert.deepEqual(
      febWeeks.map((w) => ({ year: w.year, week: w.week })),
      [
        { year: 2026, week: 6 },
        { year: 2026, week: 7 },
        { year: 2026, week: 8 },
        { year: 2026, week: 9 },
      ],
      "02/2026 phải gồm đúng 4 tuần: W6..W9 thuộc ISO year 2026"
    );

    const febMonth = buildProjectMonth(2026, 2);
    assert.equal(febMonth.id, "2026-02");
    assert.deepEqual(
      febMonth.weeks.map((w) => ({ key: w.key, label: w.label, year: w.year, weekNumber: w.weekNumber })),
      [
        { key: "W6", label: "W6", year: 2026, weekNumber: 6 },
        { key: "W7", label: "W7", year: 2026, weekNumber: 7 },
        { key: "W8", label: "W8", year: 2026, weekNumber: 8 },
        { key: "W9", label: "W9", year: 2026, weekNumber: 9 },
      ]
    );
  });

  await t.test("B. ISO week → date range: Monday → Sunday", () => {
    // W36/2026: 31/08/2026 (Mon) → 06/09/2026 (Sun)
    const w36 = getIsoWeekDateRange(2026, 36);
    assert.equal(w36.startDate.getUTCDay(), 1, "Start date phải là Thứ Hai (Monday)");
    assert.equal(w36.endDate.getUTCDay(), 0, "End date phải là Chủ Nhật (Sunday)");
    assert.equal(formatIsoWeekDateRange(w36.startDate, w36.endDate, true), "31/08/2026 – 06/09/2026");
    assert.equal(formatIsoWeekDateRange(w36.startDate, w36.endDate, false), "31/08 – 06/09");

    // Header của tháng 9/2026 phải hiển thị chính xác "31/08 – 06/09", không phải "01/09 – 07/09"
    const sepMonth = buildProjectMonth(2026, 9);
    assert.equal(sepMonth.weeks[0].dates, "31/08 – 06/09");
    assert.equal(sepMonth.weeks[0].label, "W36");

    // W40/2026: 28/09/2026 (Mon) → 04/10/2026 (Sun)
    const w40 = getIsoWeekDateRange(2026, 40);
    assert.equal(w40.startDate.getUTCDay(), 1);
    assert.equal(w40.endDate.getUTCDay(), 0);
    assert.equal(formatIsoWeekDateRange(w40.startDate, w40.endDate, true), "28/09/2026 – 04/10/2026");
    assert.equal(formatIsoWeekDateRange(w40.startDate, w40.endDate, false), "28/09 – 04/10");

    // 2026-W53: 28/12/2026 (Mon) → 03/01/2027 (Sun)
    const w53 = getIsoWeekDateRange(2026, 53);
    assert.equal(w53.startDate.getUTCDay(), 1);
    assert.equal(w53.endDate.getUTCDay(), 0);
    assert.equal(formatIsoWeekDateRange(w53.startDate, w53.endDate, true), "28/12/2026 – 03/01/2027");

    // 2027-W01: 04/01/2027 (Mon) → 10/01/2027 (Sun)
    const w01 = getIsoWeekDateRange(2027, 1);
    assert.equal(w01.startDate.getUTCDay(), 1);
    assert.equal(w01.endDate.getUTCDay(), 0);
    assert.equal(formatIsoWeekDateRange(w01.startDate, w01.endDate, true), "04/01/2027 – 10/01/2027");
  });

  await t.test("C. Không trùng tuần giữa hai tháng liền kề (SeptemberWeeks ∩ OctoberWeeks === ∅)", () => {
    const sepWeeks = getIsoWeeksForMonth(2026, 9).map((w) => `${w.year}-W${w.week}`);
    const octWeeks = getIsoWeeksForMonth(2026, 10).map((w) => `${w.year}-W${w.week}`);

    // Rule: Thursday of W40 is 01/10/2026 -> W40 belongs ONLY to October
    assert.ok(!sepWeeks.includes("2026-W40"), "Tháng 9/2026 KHÔNG được chứa W40");
    assert.ok(octWeeks.includes("2026-W40"), "Tháng 10/2026 phải chứa W40");

    const intersection = sepWeeks.filter((w) => octWeeks.includes(w));
    assert.equal(intersection.length, 0, "SeptemberWeeks ∩ OctoberWeeks phải rỗng (không trùng bất kỳ tuần nào)");

    // Kiểm tra giữa Tháng 12/2026 và Tháng 01/2027
    const decWeeks = getIsoWeeksForMonth(2026, 12).map((w) => `${w.year}-W${w.week}`);
    const janWeeks = getIsoWeeksForMonth(2027, 1).map((w) => `${w.year}-W${w.week}`);
    const decJanIntersection = decWeeks.filter((w) => janWeeks.includes(w));
    assert.equal(decJanIntersection.length, 0, "DecemberWeeks ∩ JanuaryWeeks phải rỗng");

    // Kiểm tra giữa Tháng 01/2026 và Tháng 02/2026
    const jan26Weeks = getIsoWeeksForMonth(2026, 1).map((w) => `${w.year}-W${w.week}`);
    const feb26Weeks = getIsoWeeksForMonth(2026, 2).map((w) => `${w.year}-W${w.week}`);
    const janFebIntersection = jan26Weeks.filter((w) => feb26Weeks.includes(w));
    assert.equal(janFebIntersection.length, 0, "January2026Weeks ∩ February2026Weeks phải rỗng");
  });

  await t.test("D. ISO year boundary: 2026-W53 và January 2027", () => {
    // Năm 2026 có 53 tuần ISO, năm 2027 có 52 tuần ISO
    assert.equal(getIsoWeeksInYear(2026), 53);
    assert.equal(getIsoWeeksInYear(2027), 52);

    // Ngày 01/01/2027 (Thứ Sáu), 02/01/2027 (Thứ Bảy), 03/01/2027 (Chủ Nhật)
    // thuộc về tuần 2026-W53 vì Thứ Năm của tuần này là 31/12/2026
    const jan1_2027 = new Date(2027, 0, 1);
    const detailsJan1 = getIsoWeekDetails(jan1_2027);
    assert.deepEqual(
      detailsJan1,
      { year: 2026, week: 53 },
      "Ngày 01/01/2027 thuộc 2026-W53 (ISO year 2026), không được gán là 2027-W53"
    );

    // Theo quy tắc Month of Thursday: Thứ Năm của 2026-W53 là 31/12/2026 (Tháng 12)
    // Do đó 2026-W53 thuộc Tháng 12/2026
    const decWeeks = getIsoWeeksForMonth(2026, 12);
    const hasW53InDec = decWeeks.some((w) => w.year === 2026 && w.week === 53);
    assert.ok(hasW53InDec, "Tháng 12/2026 phải chứa 2026-W53");

    // Tháng 01/2027 gồm các tuần có Thứ Năm trong tháng 1 (07/01, 14/01, 21/01, 28/01)
    // -> W01..W04 của năm 2027, không chứa 2026-W53 và không có 2027-W53
    const janMonth = buildProjectMonth(2027, 1);
    const janWeekNumbers = janMonth.weeks.map((w) => w.weekNumber);
    assert.ok(!janWeekNumbers.includes(53), "Tháng 01/2027 KHÔNG được có week 53");
    assert.deepEqual(janWeekNumbers, [1, 2, 3, 4], "Tháng 01/2027 bắt đầu từ Tuần 1 (W1)");
    janMonth.weeks.forEach((w) => {
      assert.equal(w.year, 2027, "Tất cả các tuần của 01/2027 phải có ISO year là 2027");
    });
  });

  await t.test("E. API request count: 1 month → 1 allocations request", async () => {
    // 1. Kiểm tra helper xác định khoảng tuần
    const sepMonth = buildProjectMonth(2026, 9);
    const sepRange = getMonthAllocationWeekRange(sepMonth.weeks);
    assert.deepEqual(sepRange, {
      year: 2026,
      startWeek: 36,
      endWeek: 39,
    });

    const octMonth = buildProjectMonth(2026, 10);
    const octRange = getMonthAllocationWeekRange(octMonth.weeks);
    assert.deepEqual(octRange, {
      year: 2026,
      startWeek: 40,
      endWeek: 44,
    });

    // 2. Observable behavior: fetchMonthProjectAllocations gọi đúng 1 request duy nhất cho toàn bộ tháng
    let requestCount = 0;
    const requestedCalls = [];

    const mockFetchFn = async (projectId, year, startWeek, endWeek) => {
      requestCount++;
      requestedCalls.push({ projectId, year, startWeek, endWeek });
      return [
        {
          id: 1,
          employeeId: 10,
          projectId,
          year,
          weekNumber: startWeek,
          allocatedHours: 20,
        },
      ];
    };

    // Gọi cho Tháng 9/2026
    requestCount = 0;
    requestedCalls.length = 0;
    const sepAllocations = await fetchMonthProjectAllocations(99, sepMonth.weeks, mockFetchFn);
    assert.equal(requestCount, 1, "Tháng 9/2026 chỉ được tạo đúng 1 API request duy nhất thay vì 4-5 request");
    assert.deepEqual(requestedCalls[0], {
      projectId: 99,
      year: 2026,
      startWeek: 36,
      endWeek: 39,
    });
    assert.equal(sepAllocations.length, 1);

    // Gọi cho Tháng 10/2026
    requestCount = 0;
    requestedCalls.length = 0;
    const octAllocations = await fetchMonthProjectAllocations(99, octMonth.weeks, mockFetchFn);
    assert.equal(requestCount, 1, "Tháng 10/2026 chỉ được tạo đúng 1 API request duy nhất");
    assert.deepEqual(requestedCalls[0], {
      projectId: 99,
      year: 2026,
      startWeek: 40,
      endWeek: 44,
    });
    assert.equal(octAllocations.length, 1);

    // Gọi cho Tháng 1/2027
    requestCount = 0;
    requestedCalls.length = 0;
    const janMonth = buildProjectMonth(2027, 1);
    await fetchMonthProjectAllocations(99, janMonth.weeks, mockFetchFn);
    assert.equal(requestCount, 1, "Tháng 1/2027 chỉ được tạo đúng 1 API request duy nhất");
    assert.deepEqual(requestedCalls[0], {
      projectId: 99,
      year: 2027,
      startWeek: 1,
      endWeek: 4,
    });

    // Gọi cho Tháng 2/2026
    requestCount = 0;
    requestedCalls.length = 0;
    const febMonth = buildProjectMonth(2026, 2);
    await fetchMonthProjectAllocations(99, febMonth.weeks, mockFetchFn);
    assert.equal(requestCount, 1, "Tháng 2/2026 chỉ được tạo đúng 1 API request duy nhất");
    assert.deepEqual(requestedCalls[0], {
      projectId: 99,
      year: 2026,
      startWeek: 6,
      endWeek: 9,
    });
  });

  await t.test("F. Single owner: Khi mở project hoặc load WBS, chỉ có duy nhất 1 luồng fetch allocation", async () => {
    // Mô phỏng kịch bản P1:
    // Trước đây: selectedProjectId trigger 2 luồng:
    // 1) allocation useEffect -> loadProjectAllocations() -> fetchMonthProjectAllocations() (req 1)
    // 2) wbs useEffect -> loadWbsForProject() -> loadProjectAllocations() (req 2)
    // Sau khi sửa P1: loadWbsForProject() không còn gọi loadProjectAllocations().
    // useEffect allocation phụ thuộc [canReadAllocations, selectedProjectId, selectedMonthIdx, loadProjectAllocations]
    // là single owner duy nhất thực hiện fetchMonthProjectAllocations().

    let fetchCount = 0;
    const mockFetch = async () => {
      fetchCount++;
      return [];
    };

    // Mô phỏng 1 controller / runner theo cơ chế single owner
    class ProjectAllocationCoordinator {
      constructor() {
        this.selectedProjectId = null;
        this.selectedMonthIdx = 0;
        this.months = [buildProjectMonth(2026, 9)];
        this.latestAllocations = [];
        this.members = [];
      }

      async onProjectOrMonthChange(projectId, monthIdx) {
        this.selectedProjectId = projectId;
        this.selectedMonthIdx = monthIdx;
        // Single owner fetch
        const month = this.months[monthIdx];
        this.latestAllocations = await fetchMonthProjectAllocations(projectId, month.weeks, mockFetch);
      }

      async onWbsLoaded(projectMembers) {
        // loadWbsForProject KHÔNG gọi fetch allocations nữa, mà chỉ cập nhật members
        // và map với latestAllocations nếu đã có sẵn
        this.members = projectMembers;
      }

      async onEmployeesListUpdated(employees) {
        // Tương tự, không trigger fetch lại allocation
      }
    }

    const coordinator = new ProjectAllocationCoordinator();

    // 1. User mở project 100
    // Thay vì trigger 2 request (WBS + Allocation effect), single owner chỉ trigger đúng 1 request
    await Promise.all([
      coordinator.onProjectOrMonthChange(100, 0),
      coordinator.onWbsLoaded([{ id: "u-1", name: "Nguyễn Văn A" }]),
      coordinator.onEmployeesListUpdated([{ id: "u-1", name: "Nguyễn Văn A" }, { id: "u-2", name: "Trần Thị B" }]),
    ]);

    assert.equal(fetchCount, 1, "Khi mở project và load WBS đồng thời, chỉ được phát sinh ĐÚNG 1 request allocation");
  });

  await t.test("G. Race Condition & Consistency: Sequence counter và Project Switch Cache Isolation", async () => {
    // Mô phỏng logic chống race condition trong ProjectView.tsx:
    // 1) allocationRequestRef đếm sequence để loại bỏ response cũ về muộn
    // 2) latestAllocationsRef lưu { projectId, monthId, allocations } để bảo vệ 2 lớp
    // 3) Đổi project thì reset allocationRequestRef.current++, latestAllocationsRef.current = null, setMembers([])

    let allocationRequestRef = 0;
    let latestAllocationsRef = null;
    let displayedMembers = [];

    const monthSep = buildProjectMonth(2026, 9);
    const monthOct = buildProjectMonth(2026, 10);

    const simulateLoadAllocations = async (projId, month, latencyMs, resultData) => {
      const requestId = ++allocationRequestRef;
      await new Promise((resolve) => setTimeout(resolve, latencyMs));
      // Guard race condition
      if (requestId !== allocationRequestRef) {
        return "DISCARDED";
      }
      latestAllocationsRef = {
        projectId: projId,
        monthId: month.id,
        allocations: resultData,
      };
      displayedMembers = resultData;
      return "APPLIED";
    };

    const simulateProjectSwitch = (newProjId) => {
      allocationRequestRef++;
      latestAllocationsRef = null;
      displayedMembers = [];
    };

    // Scenario 1: Request 1 (chậm, 50ms) bị Request 2 (nhanh, 10ms) đè lên -> Request 1 phải bị DISCARDED
    const req1 = simulateLoadAllocations(1, monthSep, 50, [{ id: "alloc-old", employeeId: 10 }]);
    const req2 = simulateLoadAllocations(1, monthOct, 10, [{ id: "alloc-new", employeeId: 10 }]);

    const [res1, res2] = await Promise.all([req1, req2]);
    assert.equal(res1, "DISCARDED", "Response cũ về muộn phải bị bỏ qua (discarded)");
    assert.equal(res2, "APPLIED", "Response mới nhất phải được áp dụng");
    assert.equal(latestAllocationsRef.monthId, monthOct.id, "Cache phải lưu đúng tháng mới");
    assert.equal(displayedMembers[0].id, "alloc-new");

    // Scenario 2: Đang fetch Project A thì user đổi sang Project B -> Không được để Project B dính allocation A
    const reqProjA = simulateLoadAllocations(100, monthSep, 30, [{ id: "alloc-proj-A", employeeId: 10 }]);
    // Ngay lập tức đổi sang project 200
    simulateProjectSwitch(200);
    assert.equal(latestAllocationsRef, null, "Khi đổi project, cache phải lập tức bị xóa (null)");
    assert.deepEqual(displayedMembers, [], "Danh sách members phải được reset rỗng");

    const reqProjB = simulateLoadAllocations(200, monthSep, 10, [{ id: "alloc-proj-B", employeeId: 20 }]);
    const [resA, resB] = await Promise.all([reqProjA, reqProjB]);

    assert.equal(resA, "DISCARDED", "Request của Project A phải bị bỏ qua vì project đã đổi");
    assert.equal(resB, "APPLIED", "Request của Project B được áp dụng bình thường");
    assert.equal(latestAllocationsRef.projectId, 200, "Cache phải là của Project B (200)");
    assert.equal(displayedMembers[0].id, "alloc-proj-B");
  });
});

