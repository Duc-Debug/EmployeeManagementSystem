import { test, describe } from "node:test";
import assert from "node:assert/strict";

import {
  calculateWeeklyForecast,
  canAccessCapacityForecastTab,
  visibleOrgUnitIdsForRole,
} from "../lib/api/capacity-forecast.ts";

describe("Capacity Forecast Report Logic Tests (NCL-10-CN-004)", () => {
  test("BR-03 & BR-04: Tách riêng giờ giữ chỗ khỏi giờ cam kết chính thức", () => {
    const res = calculateWeeklyForecast(400, 300, 40);
    assert.equal(res.committedRemainingHours, 100);
    assert.equal(res.projectedRemainingHours, 60);
    assert.equal(res.committedUtilization, 75.0);
    assert.equal(res.projectedUtilization, 85.0);
    assert.equal(res.status, "NEAR_FULL");
  });

  test("BR-05: Giữ giá trị âm cho giờ còn trống khi vượt năng lực", () => {
    const res = calculateWeeklyForecast(160, 150, 30);
    assert.equal(res.committedRemainingHours, 10);
    assert.equal(res.projectedRemainingHours, -20);
    assert.equal(res.committedUtilization, 93.8);
    assert.equal(res.projectedUtilization, 112.5);
    assert.equal(res.status, "OVER_CAPACITY");
  });

  test("BR-06: Xử lý an toàn khi availableHours = 0", () => {
    const zeroAvailNoAlloc = calculateWeeklyForecast(0, 0, 0);
    assert.equal(zeroAvailNoAlloc.committedUtilization, 0.0);
    assert.equal(zeroAvailNoAlloc.projectedUtilization, 0.0);
    assert.equal(zeroAvailNoAlloc.status, "AVAILABLE");

    const zeroAvailWithAlloc = calculateWeeklyForecast(0, 20, 0);
    assert.equal(zeroAvailWithAlloc.committedUtilization, null);
    assert.equal(zeroAvailWithAlloc.projectedUtilization, null);
    assert.equal(zeroAvailWithAlloc.status, "OVER_CAPACITY");
  });

  test("BR-07: Menu dùng permission matrix từ backend thay vì hard-code role", () => {
    assert.equal(canAccessCapacityForecastTab(["CAPACITY_FORECAST_REPORT_READ"]), true);
    assert.equal(canAccessCapacityForecastTab(["PROJECT_READ"]), false);
    assert.equal(canAccessCapacityForecastTab([]), false);
    assert.equal(canAccessCapacityForecastTab(undefined), false);
  });

  test("BR-08: Phân quyền vai trò - Chỉ VT-01 và VT-03 được xem, chặn VT-02, VT-04, VT-05, VT-06", () => {
    // Trường hợp kiểm tra theo role fallback (chưa nạp permissions)
    assert.equal(canAccessCapacityForecastTab(null, "VT-01"), true);
    assert.equal(canAccessCapacityForecastTab(null, "VT-03"), true);
    assert.equal(canAccessCapacityForecastTab(null, "VT-02"), false);
    assert.equal(canAccessCapacityForecastTab(null, "VT-04"), false);
    assert.equal(canAccessCapacityForecastTab(null, "VT-05"), false);
    assert.equal(canAccessCapacityForecastTab(null, "VT-06"), false);
    assert.equal(canAccessCapacityForecastTab(null, "ADMIN"), false);

    // Trường hợp có permissions cụ thể từ backend
    assert.equal(canAccessCapacityForecastTab(["CAPACITY_FORECAST_REPORT_READ"], "VT-06"), true);
    assert.equal(canAccessCapacityForecastTab(["USER_READ", "ORG_UNIT_MANAGE"], "VT-06"), false);
    assert.equal(canAccessCapacityForecastTab(["PROJECT_READ", "PROJECT_CREATE"], "VT-02"), false);
    assert.equal(canAccessCapacityForecastTab(["WORK_LOG_CREATE"], "VT-04"), false);
    assert.equal(canAccessCapacityForecastTab(["EMPLOYEE_UPDATE"], "VT-05"), false);
  });

  test("VT-03 chỉ nhìn thấy đơn vị trong nhánh data scope", () => {
    const tree = [
      { id: 1, children: [{ id: 2, children: [{ id: 3, children: [] }] }] },
      { id: 4, children: [] }
    ];

    assert.deepEqual(visibleOrgUnitIdsForRole(tree, "VT-03", 2), [2, 3]);
    assert.deepEqual(visibleOrgUnitIdsForRole(tree, "VT-01", null), [1, 2, 3, 4]);
  });
});
