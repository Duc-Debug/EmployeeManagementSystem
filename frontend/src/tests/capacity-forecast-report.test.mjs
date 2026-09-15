import { test, describe } from "node:test";
import assert from "node:assert/strict";

function calculateWeeklyForecast(availableHours, committedHours, reservedHours) {
  const committedRemainingHours = availableHours - committedHours;
  const projectedRemainingHours = availableHours - committedHours - reservedHours;

  let committedUtilization = null;
  if (availableHours > 0) {
    committedUtilization = Number(((committedHours / availableHours) * 100).toFixed(1));
  } else if (committedHours === 0) {
    committedUtilization = 0.0;
  }

  let projectedUtilization = null;
  if (availableHours > 0) {
    projectedUtilization = Number((((committedHours + reservedHours) / availableHours) * 100).toFixed(1));
  } else if (committedHours + reservedHours === 0) {
    projectedUtilization = 0.0;
  }

  let status = "AVAILABLE";
  if (
    projectedRemainingHours < 0 ||
    (availableHours === 0 && committedHours + reservedHours > 0) ||
    (projectedUtilization !== null && projectedUtilization > 100)
  ) {
    status = "OVER_CAPACITY";
  } else if (projectedUtilization !== null && projectedUtilization >= 80) {
    status = "NEAR_FULL";
  }

  return {
    committedRemainingHours,
    projectedRemainingHours,
    committedUtilization,
    projectedUtilization,
    status
  };
}

function canAccessCapacityForecastTab(roleCode) {
  if (!roleCode) return false;
  const normalized = roleCode.toUpperCase().replace(/_/g, "-");
  return ["VT-01", "VT-03"].includes(normalized);
}

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

  test("BR-07: Phân quyền menu hiển thị cho VT-01 & VT-03, chặn các vai trò khác", () => {
    assert.equal(canAccessCapacityForecastTab("VT-01"), true);
    assert.equal(canAccessCapacityForecastTab("VT-03"), true);
    assert.equal(canAccessCapacityForecastTab("VT-02"), false);
    assert.equal(canAccessCapacityForecastTab("VT-04"), false);
    assert.equal(canAccessCapacityForecastTab("VT-05"), false);
  });
});
