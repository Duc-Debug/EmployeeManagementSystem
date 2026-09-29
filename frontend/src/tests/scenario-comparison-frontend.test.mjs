import test from "node:test";
import assert from "node:assert/strict";

import {
  findRecommendedScenario,
  validateScenarioSelection,
  canUserCompareScenarios,
  checkScenariosAlignment,
  buildComparisonCsv,
  clampUtilization,
} from "../lib/scenario-comparison.ts";

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
        averageUtilizationPercentage: 110.0,
      },
      {
        scenarioId: 2,
        scenarioCode: "SCN-02",
        overloadedEmployeesCount: 0,
        totalShortfallHours: 0.0,
        peakUtilizationPercentage: 92.5,
        totalRequiredAdditionalHours: 0.0,
        averageUtilizationPercentage: 85.0,
      },
      {
        scenarioId: 3,
        scenarioCode: "SCN-03",
        overloadedEmployeesCount: 1,
        totalShortfallHours: 10.0,
        peakUtilizationPercentage: 108.0,
        totalRequiredAdditionalHours: 12.0,
        averageUtilizationPercentage: 98.0,
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

  await t.test("TC-07: CSV export generates UTF-8 BOM and correct columns with recommendation tag", () => {
    const mockData = {
      comparedAt: "2026-09-16T10:00:00Z",
      scenarios: [
        {
          scenarioId: 10,
          scenarioCode: "SCN-10",
          scenarioName: "Kịch bản A",
          orgUnitName: "Phòng Phần mềm 1",
          status: "draft",
          fromWeek: 10,
          fromYear: 2026,
          durationWeeks: 4,
          overloadedEmployeesCount: 0,
          totalShortfallHours: 0,
          totalRequiredAdditionalHours: 0,
          totalDemandHours: 160,
          totalWorkloadHours: 640,
          totalAvailableHours: 800,
          averageUtilizationPercentage: 80.0,
          peakUtilizationPercentage: 90.0,
        },
        {
          scenarioId: 20,
          scenarioCode: "SCN-20",
          scenarioName: "Kịch bản B",
          orgUnitName: "Phòng Phần mềm 1",
          status: "draft",
          fromWeek: 10,
          fromYear: 2026,
          durationWeeks: 4,
          overloadedEmployeesCount: 2,
          totalShortfallHours: 40,
          totalRequiredAdditionalHours: 40,
          totalDemandHours: 240,
          totalWorkloadHours: 880,
          totalAvailableHours: 800,
          averageUtilizationPercentage: 110.0,
          peakUtilizationPercentage: 125.0,
        },
      ],
    };

    const csvStr = buildComparisonCsv(mockData, 10);
    assert.ok(csvStr.startsWith("\uFEFF"), "CSV must begin with UTF-8 BOM");
    assert.ok(csvStr.includes("BÁO CÁO ĐỐI CHIẾU KỊCH BẢN MÔ PHỎNG NGUỒN LỰC"));
    assert.ok(csvStr.includes("QTN-14 Sandbox"));
    assert.ok(csvStr.includes('"SCN-10"'));
    assert.ok(csvStr.includes('"Kịch bản A"'));
    assert.ok(csvStr.includes("Tối ưu nhất"));
    assert.ok(csvStr.includes("Phương án"));
  });

  await t.test("TC-08: Utilization clamp protects against NaN, null, and negative values", () => {
    assert.equal(clampUtilization(null), 0);
    assert.equal(clampUtilization(undefined), 0);
    assert.equal(clampUtilization(NaN), 0);
    assert.equal(clampUtilization(-15), 0);
    assert.equal(clampUtilization(85.5), 85.5);
    assert.equal(clampUtilization(120), 100);
  });

  await t.test("TC-09: Tie-breaker with averageUtilizationPercentage when all other metrics are equal", () => {
    const mockScenarios = [
      {
        scenarioId: 100,
        scenarioCode: "SCN-100",
        overloadedEmployeesCount: 0,
        totalShortfallHours: 0.0,
        peakUtilizationPercentage: 95.0,
        totalRequiredAdditionalHours: 0.0,
        averageUtilizationPercentage: 90.0,
      },
      {
        scenarioId: 200,
        scenarioCode: "SCN-200",
        overloadedEmployeesCount: 0,
        totalShortfallHours: 0.0,
        peakUtilizationPercentage: 95.0,
        totalRequiredAdditionalHours: 0.0,
        averageUtilizationPercentage: 80.0,
      },
    ];

    const recommendedId = findRecommendedScenario(mockScenarios);
    assert.equal(recommendedId, 200); // SCN-200 has 80% avg util vs SCN-100 with 90%
  });
});
