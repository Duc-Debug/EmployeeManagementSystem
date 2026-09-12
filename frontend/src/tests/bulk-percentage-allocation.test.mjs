import test from "node:test";
import assert from "node:assert/strict";

function buildBulkPayload({
  employeeId,
  projectId,
  startYear,
  startWeek,
  endYear,
  endWeek,
  allocationMode,
  allocatedHours,
  allocationPercentage,
  description,
}) {
  const payload = {
    employeeId,
    projectId,
    startYear,
    startWeek,
    endYear,
    endWeek,
    description: description || undefined,
  };

  if (allocationMode === "percentage") {
    payload.allocationPercentagePerWeek = allocationPercentage;
  } else {
    payload.allocatedHoursPerWeek = allocatedHours;
  }

  return payload;
}

function computeProjectedBulkAllocation(weeks, percentage) {
  return weeks.map((w) => {
    const netAvailable = w.netAvailableHours;
    const hours = Number(((netAvailable * percentage) / 100).toFixed(2));
    return {
      year: w.year,
      week: w.week,
      netAvailableHours: netAvailable,
      projectedHours: hours,
      percentage,
    };
  });
}

test("Bulk Percentage Allocation Tests (NCL-06-CN-007)", async (t) => {
  await t.test("TC-01: Bulk payload contains allocationPercentagePerWeek when mode is percentage", () => {
    const payload = buildBulkPayload({
      employeeId: "emp-1",
      projectId: "proj-101",
      startYear: 2026,
      startWeek: 10,
      endYear: 2026,
      endWeek: 13,
      allocationMode: "percentage",
      allocatedHours: 20,
      allocationPercentage: 50,
      description: "Allocated 50% across 4 weeks",
    });

    assert.equal(payload.allocationPercentagePerWeek, 50);
    assert.equal(payload.allocatedHoursPerWeek, undefined);
    assert.equal(payload.employeeId, "emp-1");
  });

  await t.test("TC-02: Bulk payload contains allocatedHoursPerWeek when mode is hours", () => {
    const payload = buildBulkPayload({
      employeeId: "emp-1",
      projectId: "proj-101",
      startYear: 2026,
      startWeek: 10,
      endYear: 2026,
      endWeek: 13,
      allocationMode: "hours",
      allocatedHours: 20,
      allocationPercentage: 50,
      description: "Allocated 20h across 4 weeks",
    });

    assert.equal(payload.allocatedHoursPerWeek, 20);
    assert.equal(payload.allocationPercentagePerWeek, undefined);
  });

  await t.test("TC-03: Dynamic hours projection varies with each week's net availability", () => {
    const weeks = [
      { year: 2026, week: 1, netAvailableHours: 40.0 },
      { year: 2026, week: 2, netAvailableHours: 32.0 }, // 1 day holiday
      { year: 2026, week: 3, netAvailableHours: 40.0 },
      { year: 2026, week: 4, netAvailableHours: 24.0 }, // 2 days leave
    ];

    const result = computeProjectedBulkAllocation(weeks, 50.0); // 50%
    assert.deepEqual(result, [
      { year: 2026, week: 1, netAvailableHours: 40.0, projectedHours: 20.0, percentage: 50.0 },
      { year: 2026, week: 2, netAvailableHours: 32.0, projectedHours: 16.0, percentage: 50.0 },
      { year: 2026, week: 3, netAvailableHours: 40.0, projectedHours: 20.0, percentage: 50.0 },
      { year: 2026, week: 4, netAvailableHours: 24.0, projectedHours: 12.0, percentage: 50.0 },
    ]);
  });
});
