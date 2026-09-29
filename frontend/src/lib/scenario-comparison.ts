/**
 * Helper logic and validation policies for Scenario Comparison (NCL-08-CN-004 / QTN-14)
 */

export interface ScenarioComparisonItem {
  scenarioId: number;
  scenarioCode?: string;
  scenarioName?: string;
  orgUnitId?: number;
  orgUnitName?: string;
  status?: string;
  fromYear?: number;
  fromWeek?: number;
  durationWeeks?: number;
  overloadedEmployeesCount: number;
  totalShortfallHours?: number;
  peakUtilizationPercentage?: number;
  totalRequiredAdditionalHours?: number;
  totalDemandHours?: number;
  totalWorkloadHours?: number;
  totalAvailableHours?: number;
  averageUtilizationPercentage?: number;
}

export function findRecommendedScenario(scenarios: ScenarioComparisonItem[] | null | undefined): number | null {
  if (!scenarios || scenarios.length === 0) return null;
  const sorted = [...scenarios].sort((a, b) => {
    // 1. Ít nhân sự quá tải hơn
    if (a.overloadedEmployeesCount !== b.overloadedEmployeesCount) {
      return a.overloadedEmployeesCount - b.overloadedEmployeesCount;
    }
    // 2. Tổng giờ thiếu hụt ít hơn
    if ((a.totalShortfallHours ?? 0) !== (b.totalShortfallHours ?? 0)) {
      return (a.totalShortfallHours ?? 0) - (b.totalShortfallHours ?? 0);
    }
    // 3. Đỉnh tải thấp hơn
    if ((a.peakUtilizationPercentage ?? 0) !== (b.peakUtilizationPercentage ?? 0)) {
      return (a.peakUtilizationPercentage ?? 0) - (b.peakUtilizationPercentage ?? 0);
    }
    // 4. Giờ làm thêm cần thiết ít hơn
    if ((a.totalRequiredAdditionalHours ?? 0) !== (b.totalRequiredAdditionalHours ?? 0)) {
      return (a.totalRequiredAdditionalHours ?? 0) - (b.totalRequiredAdditionalHours ?? 0);
    }
    // 5. Tỷ lệ tải trung bình tối ưu hơn
    return (a.averageUtilizationPercentage ?? 0) - (b.averageUtilizationPercentage ?? 0);
  });
  return sorted[0]?.scenarioId ?? null;
}

export function validateScenarioSelection(scenarioIds: unknown) {
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

export function canUserCompareScenarios(roleCode?: string | null, permissions: string[] = []): boolean {
  const normalized = roleCode ? roleCode.toUpperCase().replace(/_/g, "-") : "";
  const isVT01 =
    normalized === "VT-01" ||
    normalized === "ROLE-BGD" ||
    normalized === "BGD" ||
    normalized === "DIRECTOR";
  return isVT01 || permissions.includes("RESOURCE_SCENARIO_COMPARE");
}

export function checkScenariosAlignment(scenarios: ScenarioComparisonItem[] | null | undefined) {
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

export function buildComparisonCsv(
  data: { comparedAt?: string; scenarios: ScenarioComparisonItem[] },
  recommendedId?: number | null
): string {
  const rows = [
    ["BÁO CÁO ĐỐI CHIẾU KỊCH BẢN MÔ PHỎNG NGUỒN LỰC (NCL-08-CN-004)"],
    [`Thời gian đối chiếu: ${data.comparedAt}`],
    ["Nguyên tắc QTN-14 Sandbox: Dữ liệu mô phỏng độc lập, không làm thay đổi phân bổ thật."],
    [],
    [
      "Mã kịch bản",
      "Tên kịch bản",
      "Đơn vị / Phòng ban",
      "Trạng thái",
      "Tuần bắt đầu",
      "Năm",
      "Số tuần",
      "Số nhân sự quá tải",
      "Tổng giờ thiếu hụt (h)",
      "Giờ làm thêm cần thiết (h)",
      "Giờ nhu cầu giả định (h)",
      "Tổng khối lượng (h)",
      "Giờ khả dụng (h)",
      "Tải trung bình (%)",
      "Đỉnh tải (%)",
      "Khuyến nghị",
    ],
  ];

  for (const scn of data.scenarios) {
    const isRec = scn.scenarioId === recommendedId;
    rows.push([
      `"${scn.scenarioCode}"`,
      `"${scn.scenarioName}"`,
      `"${scn.orgUnitName}"`,
      `"${scn.status}"`,
      `${scn.fromWeek}`,
      `${scn.fromYear}`,
      `${scn.durationWeeks}`,
      `${scn.overloadedEmployeesCount}`,
      `${scn.totalShortfallHours ?? 0}`,
      `${scn.totalRequiredAdditionalHours ?? 0}`,
      `${scn.totalDemandHours ?? 0}`,
      `${scn.totalWorkloadHours ?? 0}`,
      `${scn.totalAvailableHours ?? 0}`,
      `${Number(scn.averageUtilizationPercentage ?? 0).toFixed(1)}%`,
      `${Number(scn.peakUtilizationPercentage ?? 0).toFixed(1)}%`,
      isRec ? "Tối ưu nhất" : "Phương án",
    ]);
  }

  return "\uFEFF" + rows.map((r) => r.join(",")).join("\r\n");
}

export function clampUtilization(val: unknown): number {
  return Math.max(0, Math.min(Number(val || 0), 100));
}
