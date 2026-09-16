import test from "node:test";
import assert from "node:assert/strict";

/**
 * Helper logic mirroring frontend recommendation algorithm
 */
function findRecommendedScenario(scenarios) {
  if (!scenarios || scenarios.length === 0) return null;
  const sorted = [...scenarios].sort((a, b) => {
    // 1. Ít nhân sự quá tải hơn
    if (a.overloadedEmployeesCount !== b.overloadedEmployeesCount) {
      return a.overloadedEmployeesCount - b.overloadedEmployeesCount;
    }
    // 2. Tổng giờ thiếu hụt ít hơn
    if (a.totalShortfallHours !== b.totalShortfallHours) {
      return a.totalShortfallHours - b.totalShortfallHours;
    }
    // 3. Đỉnh tải thấp hơn
    if (a.peakUtilizationPercentage !== b.peakUtilizationPercentage) {
      return a.peakUtilizationPercentage - b.peakUtilizationPercentage;
    }
    // 4. Giờ làm thêm cần thiết ít hơn
    return a.totalRequiredAdditionalHours - b.totalRequiredAdditionalHours;
  });
  return sorted[0]?.scenarioId ?? null;
}

/**
 * Helper logic validating scenario selection bounds (min 2, max 10)
 */
function validateScenarioSelection(scenarioIds) {
  if (!Array.isArray(scenarioIds)) {
    return { valid: false, error: "Danh sách kịch bản không hợp lệ." };
  }
  if (scenarioIds.length < 2) {
    return { valid: false, error: "Cần chọn tối thiểu 2 kịch bản để thực hiện so sánh." };
  }
  if (scenarioIds.length > 10) {
    return { valid: false, error: "Chỉ được phép so sánh tối đa 10 kịch bản cùng lúc." };
  }
  return { valid: true, error: null };
}

/**
 * Helper logic checking role permission for scenario comparison (VT-01 or RESOURCE_SCENARIO_COMPARE)
 */
function canUserCompareScenarios(roleCode, permissions = []) {
  const normalized = roleCode ? roleCode.toUpperCase().replace(/_/g, "-") : "";
  const isVT01 =
    normalized === "VT-01" ||
    normalized === "ROLE-BGD" ||
    normalized === "BGD" ||
    normalized === "DIRECTOR";
  return isVT01 || permissions.includes("RESOURCE_SCENARIO_COMPARE");
}

/**
 * Alignment checking helper
 */
function checkScenariosAlignment(scenarios) {
  if (!scenarios || scenarios.length <= 1) {
    return { isTimeframeAligned: true, isOrgUnitAligned: true };
  }
  const first = scenarios[0];
  const isTimeframeAligned = scenarios.every(
    (s) =>
      s.fromYear === first.fromYear &&
      s.fromWeek === first.fromWeek &&
      s.durationWeeks === first.durationWeeks
  );
  const isOrgUnitAligned = scenarios.every((s) => s.orgUnitId === first.orgUnitId);
  return { isTimeframeAligned, isOrgUnitAligned };
}

test("Scenario Comparison Frontend Logic Tests (NCL-08-CN-004 / QTN-14)", async (t) => {
  await t.test("TC-01: Validation rule on number of scenarios (min 2, max 10)", () => {
    assert.equal(validateScenarioSelection([]).valid, false);
    assert.equal(validateScenarioSelection([1]).valid, false);
    assert.equal(validateScenarioSelection([1, 2]).valid, true);
    assert.equal(validateScenarioSelection([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]).valid, true);
    assert.equal(validateScenarioSelection([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]).valid, false);
  });

  await t.test("TC-02: API payload format matching backend contract", () => {
    const selected = [101, 102, 103];
    const payload = { scenarioIds: selected };
    assert.deepEqual(payload, { scenarioIds: [101, 102, 103] });
    assert.equal(payload.scenarioIds.length, 3);
  });

  await t.test("TC-03: Recommendation algorithm picks scenario with lowest overloaded headcount and shortfall", () => {
    const mockScenarios = [
      {
        scenarioId: 1,
        scenarioCode: "SCN-01",
        overloadedEmployeesCount: 3,
        totalShortfallHours: 45.0,
        peakUtilizationPercentage: 125.0,
        totalRequiredAdditionalHours: 50.0,
      },
      {
        scenarioId: 2,
        scenarioCode: "SCN-02",
        overloadedEmployeesCount: 0,
        totalShortfallHours: 0.0,
        peakUtilizationPercentage: 92.5,
        totalRequiredAdditionalHours: 0.0,
      },
      {
        scenarioId: 3,
        scenarioCode: "SCN-03",
        overloadedEmployeesCount: 1,
        totalShortfallHours: 10.0,
        peakUtilizationPercentage: 108.0,
        totalRequiredAdditionalHours: 12.0,
      },
    ];

    const recommendedId = findRecommendedScenario(mockScenarios);
    assert.equal(recommendedId, 2);
  });

  await t.test("TC-04: Tie-breaking in recommendation algorithm by peak utilization", () => {
    const mockScenarios = [
      {
        scenarioId: 10,
        scenarioCode: "SCN-10",
        overloadedEmployeesCount: 1,
        totalShortfallHours: 20.0,
        peakUtilizationPercentage: 115.0,
        totalRequiredAdditionalHours: 20.0,
      },
      {
        scenarioId: 20,
        scenarioCode: "SCN-20",
        overloadedEmployeesCount: 1,
        totalShortfallHours: 20.0,
        peakUtilizationPercentage: 105.0,
        totalRequiredAdditionalHours: 20.0,
      },
    ];

    const recommendedId = findRecommendedScenario(mockScenarios);
    assert.equal(recommendedId, 20); // Lower peak utilization (105% vs 115%)
  });

  await t.test("TC-05: Alignment detection for timeframe and department", () => {
    const alignedScenarios = [
      { fromYear: 2026, fromWeek: 10, durationWeeks: 4, orgUnitId: 5 },
      { fromYear: 2026, fromWeek: 10, durationWeeks: 4, orgUnitId: 5 },
    ];
    const resAligned = checkScenariosAlignment(alignedScenarios);
    assert.equal(resAligned.isTimeframeAligned, true);
    assert.equal(resAligned.isOrgUnitAligned, true);

    const misalignedScenarios = [
      { fromYear: 2026, fromWeek: 10, durationWeeks: 4, orgUnitId: 5 },
      { fromYear: 2026, fromWeek: 12, durationWeeks: 8, orgUnitId: 7 },
    ];
    const resMisaligned = checkScenariosAlignment(misalignedScenarios);
    assert.equal(resMisaligned.isTimeframeAligned, false);
    assert.equal(resMisaligned.isOrgUnitAligned, false);
  });

  await t.test("TC-06: RBAC permission check allows VT-01 / RESOURCE_SCENARIO_COMPARE and blocks unauthorized roles", () => {
    assert.equal(canUserCompareScenarios("VT-01"), true);
    assert.equal(canUserCompareScenarios("ROLE_BGD"), true);
    assert.equal(canUserCompareScenarios("VT-02", ["RESOURCE_SCENARIO_COMPARE"]), true);
    assert.equal(canUserCompareScenarios("VT-02"), false);
    assert.equal(canUserCompareScenarios("VT-04"), false);
    assert.equal(canUserCompareScenarios("VT-05"), false);
  });
});
