import { test, describe } from "node:test";
import assert from "node:assert/strict";

function calculateBillableRate(standardHours, holidayHours, approvedLeaveHours, billableHours, nonBillableHours) {
  const netAvailableHours = Math.max(0, standardHours - holidayHours - approvedLeaveHours);
  const totalActualHours = billableHours + nonBillableHours;

  let billableRate = null;
  let hasAvailableHours = netAvailableHours > 0;

  if (hasAvailableHours) {
    billableRate = Number(((billableHours / netAvailableHours) * 100).toFixed(1));
  }

  let status = "LOW_UTILIZATION";
  if (!hasAvailableHours && approvedLeaveHours >= standardHours) {
    status = "ON_LEAVE";
  } else if (billableRate === null || billableHours === 0) {
    status = "NO_BILLABLE_HOURS";
  } else if (billableRate >= 85.0) {
    status = "HIGH_UTILIZATION";
  } else if (billableRate >= 70.0) {
    status = "OPTIMAL";
  }

  return {
    netAvailableHours,
    totalActualHours,
    billableRate,
    hasAvailableHours,
    status
  };
}

function canAccessBillableRateTab(roleCode, permissions) {
  const normalized = roleCode ? roleCode.toUpperCase().replace(/_/g, "-") : "";
  return (
    permissions?.includes("BILLABLE_HOURS_REPORT_READ") === true ||
    ["VT-01", "VT-03", "VT-06", "ROLE-ADMIN", "ADMIN"].includes(normalized)
  );
}

describe("Billable Rate Report Logic Tests (NCL-10-CN-002)", () => {
  test("TC-01: Luồng thành công - 120h tính phí trên 160h khả dụng tính ra tỷ lệ 75.0%", () => {
    const res = calculateBillableRate(160, 0, 0, 120, 10);
    assert.equal(res.netAvailableHours, 160);
    assert.equal(res.totalActualHours, 130);
    assert.equal(res.billableRate, 75.0);
    assert.equal(res.hasAvailableHours, true);
    assert.equal(res.status, "OPTIMAL");
  });

  test("TC-02: Ngoại lệ nghỉ phép - Đơn nghỉ phép cả tuần (40h) được trừ khỏi mẫu số", () => {
    // 160h chuẩn, 40h nghỉ phép -> 120h khả dụng; làm 90h tính phí -> 90 / 120 = 75.0%
    const res = calculateBillableRate(160, 0, 40, 90, 0);
    assert.equal(res.netAvailableHours, 120);
    assert.equal(res.totalActualHours, 90);
    assert.equal(res.billableRate, 75.0);
    assert.equal(res.hasAvailableHours, true);
    assert.equal(res.status, "OPTIMAL");
  });

  test("Ngoại lệ: Nghỉ phép toàn bộ kỳ -> Net available = 0h -> Billable rate trả về null (N/A), status ON_LEAVE", () => {
    const res = calculateBillableRate(40, 0, 40, 0, 0);
    assert.equal(res.netAvailableHours, 0);
    assert.equal(res.billableRate, null);
    assert.equal(res.hasAvailableHours, false);
    assert.equal(res.status, "ON_LEAVE");
  });

  test("TC-03: Phân quyền - VT-01 (Ban giám đốc), VT-03 (RM), VT-06 (Admin) được phép, VT-04 / VT-05 bị từ chối", () => {
    assert.equal(canAccessBillableRateTab("VT-01", []), true);
    assert.equal(canAccessBillableRateTab("VT-03", []), true);
    assert.equal(canAccessBillableRateTab("VT-06", []), true);
    assert.equal(canAccessBillableRateTab("VT-04", ["BILLABLE_HOURS_REPORT_READ"]), true);
    assert.equal(canAccessBillableRateTab("VT-04", []), false);
    assert.equal(canAccessBillableRateTab("VT-05", []), false);
  });

  test("Tổng hợp tỷ lệ theo phòng ban và toàn công ty tính theo tổng trọng số giờ", () => {
    const emp1 = calculateBillableRate(160, 0, 0, 120, 0); // 120 / 160
    const emp2 = calculateBillableRate(160, 0, 0, 80, 0);  // 80 / 160

    const totalAvailable = emp1.netAvailableHours + emp2.netAvailableHours; // 320
    const totalBillable = 120 + 80; // 200
    const overallRate = Number(((totalBillable / totalAvailable) * 100).toFixed(1)); // 62.5%

    assert.equal(totalAvailable, 320);
    assert.equal(totalBillable, 200);
    assert.equal(overallRate, 62.5);
  });
});
