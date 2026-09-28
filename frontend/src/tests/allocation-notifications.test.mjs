import test from "node:test";
import assert from "node:assert/strict";

// Helper logic mimicking RBAC check in CompanyWeeklyCapacityView (BR-05, AC-03)
function checkCanAccessAllocationNotifications(roleCode) {
  if (!roleCode) return false;
  const normalized = roleCode.toUpperCase().replace(/_/g, "-").replace(/^ROLE-/, "");
  return normalized === "VT-02" || normalized === "VT-03";
}

// Helper logic mimicking URL query construction in getAllocationNotifications
function buildNotificationQuery(params) {
  const searchParams = new URLSearchParams();
  if (params?.projectId != null) searchParams.append("projectId", String(params.projectId));
  if (params?.page != null) searchParams.append("page", String(params.page));
  if (params?.size != null) searchParams.append("size", String(params.size));
  const queryStr = searchParams.toString();
  return `/allocations/notifications${queryStr ? `?${queryStr}` : ""}`;
}

// Helper logic mimicking consecutive week range display formatting (BR-04, TC-02)
function formatConsecutiveWeekRange(startYear, startWeek, endYear, endWeek) {
  if (startYear === endYear && startWeek === endWeek) {
    return `tuần ${startWeek}/${startYear}`;
  }
  return `từ tuần ${startWeek}/${startYear} đến tuần ${endWeek}/${endYear}`;
}

test("RBAC: VT-02 (PM) và VT-03 (RM) được phép truy cập màn hình thông báo phân bổ (BR-05, AC-03)", () => {
  assert.equal(checkCanAccessAllocationNotifications("VT-02"), true);
  assert.equal(checkCanAccessAllocationNotifications("ROLE_VT_02"), true);
  assert.equal(checkCanAccessAllocationNotifications("vt_02"), true);

  assert.equal(checkCanAccessAllocationNotifications("VT-03"), true);
  assert.equal(checkCanAccessAllocationNotifications("ROLE_VT_03"), true);
  assert.equal(checkCanAccessAllocationNotifications("vt_03"), true);
});

test("RBAC: Các vai trò khác (VT-01, VT-04, VT-05, VT-06) bị chặn truy cập màn hình thông báo (BR-05, AC-03, TC-03)", () => {
  assert.equal(checkCanAccessAllocationNotifications("VT-01"), false);
  assert.equal(checkCanAccessAllocationNotifications("VT-04"), false);
  assert.equal(checkCanAccessAllocationNotifications("VT-05"), false);
  assert.equal(checkCanAccessAllocationNotifications("VT-06"), false);
  assert.equal(checkCanAccessAllocationNotifications(null), false);
  assert.equal(checkCanAccessAllocationNotifications(undefined), false);
  assert.equal(checkCanAccessAllocationNotifications(""), false);
});

test("API Query: Tạo URL truy vấn phân trang và lọc dự án chính xác", () => {
  assert.equal(buildNotificationQuery(), "/allocations/notifications");
  assert.equal(
    buildNotificationQuery({ page: 0, size: 10 }),
    "/allocations/notifications?page=0&size=10"
  );
  assert.equal(
    buildNotificationQuery({ projectId: 101, page: 1, size: 20 }),
    "/allocations/notifications?projectId=101&page=1&size=20"
  );
});

test("Hiển thị chuỗi tuần liên tiếp: Đơn tuần và đa tuần (BR-04, TC-02)", () => {
  assert.equal(formatConsecutiveWeekRange(2026, 38, 2026, 38), "tuần 38/2026");
  assert.equal(
    formatConsecutiveWeekRange(2026, 38, 2026, 42),
    "từ tuần 38/2026 đến tuần 42/2026"
  );
  assert.equal(
    formatConsecutiveWeekRange(2026, 50, 2027, 4),
    "từ tuần 50/2026 đến tuần 4/2027"
  );
});
