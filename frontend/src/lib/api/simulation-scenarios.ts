"use client";

import { apiRequest } from "../api-client";

export interface ScenarioResult {
  id: number;
  code: string;
  name: string;
  description: string | null;
  note?: string | null;
  orgUnitId: number;
  orgUnitName: string;
  status: "draft" | "saved" | "applied" | "discarded" | string;
  viewMode?: "EDIT" | "VIEW_ONLY";
  fromYear: number;
  fromWeek: number;
  durationWeeks: number;
  baseSnapshotAt: string;
  createdBy: number;
  createdByName: string;
  createdAt: string;
  updatedAt: string | null;
  demandsCount: number;
  snapshotEmployeesCount: number;
  // Aliases for compatibility
  startYear?: number;
  startWeek?: number;
  creatorName?: string;
  demandCount?: number;
  totalSnapshotEmployees?: number;
  appliedAt?: string | null;
  targetProjectId?: number | null;
  appliedBy?: number | null;
}

export interface ScenarioDemandResult {
  id: number;
  scenarioId: number;
  demandName: string;
  roleName?: string;
  headcount: number;
  startYear: number;
  startWeek: number;
  endYear: number;
  endWeek: number;
  weekStart?: number;
  weekEnd?: number;
  hoursPerWeekPerPerson: number;
  hoursPerWeek?: number;
  totalHoursPerWeek: number;
  skillRequirement: string | null;
  requiredSkill?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface ScenarioDetailResult {
  scenario: ScenarioResult;
  demands: ScenarioDemandResult[];
}

export interface WeeklySimulationMetricResult {
  year: number;
  weekNumber: number;
  weekLabel?: string;
  snapshotAllocatedHours: number;
  demandHours: number;
  scenarioWorkloadHours: number;
  availableHours: number;
  remainingHours?: number;
  excessHours?: number;
  utilizationPercentage: number;
  status: "OVERLOADED" | "OPTIMAL" | "AVAILABLE" | "UNDERLOADED" | string;
  isOverloaded?: boolean;
  // Aliases for compatibility
  totalWorkloadHours?: number;
  availableCapacityHours?: number;
}

export interface OverloadedEmployeeResult {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  professionalRole: string;
  year: number;
  weekNumber: number;
  weekLabel?: string;
  allocatedHours: number;
  availableHours: number;
  excessHours: number;
  utilizationPercentage: number;
  status: string;
}

export interface EmployeeSnapshotCellResult {
  year: number;
  weekNumber: number;
  allocatedHours: number;
  availableHours: number;
  excessHours?: number;
  utilizationPercentage?: number;
  status?: string;
  isOverloaded?: boolean;
  // Aliases for compatibility
  snapshotAllocatedHours?: number;
  snapshotAvailableHours?: number;
}

export interface EmployeeSnapshotRowResult {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  professionalRole: string;
  cells: EmployeeSnapshotCellResult[];
  // Aliases for compatibility
  employeeName?: string;
  weeklyCells?: EmployeeSnapshotCellResult[];
}

export interface ScenarioSimulationResult {
  scenarioId: number;
  scenarioCode?: string;
  scenarioName?: string;
  orgUnitId?: number;
  orgUnitName?: string;
  status?: string;
  baseSnapshotAt?: string;
  weeklyMetrics: WeeklySimulationMetricResult[];
  overloadedEmployees: OverloadedEmployeeResult[];
  employeeSnapshots: EmployeeSnapshotRowResult[];
  overloadThreshold: number;
  idleThreshold: number;
  // Aliases for compatibility
  employeeRows?: EmployeeSnapshotRowResult[];
}

export interface CreateScenarioPayload {
  code?: string;
  name: string;
  description?: string;
  orgUnitId: number;
  fromYear: number;
  fromWeek: number;
  durationWeeks: number;
  // Aliases for compatibility
  startYear?: number;
  startWeek?: number;
}

export interface AddDemandPayload {
  demandName: string;
  roleName?: string;
  headcount: number;
  startYear: number;
  startWeek: number;
  endYear: number;
  endWeek: number;
  weekStart?: number;
  weekEnd?: number;
  hoursPerWeekPerPerson: number;
  hoursPerWeek?: number;
  skillRequirement?: string;
  requiredSkill?: string;
}

export interface UpdateDemandPayload {
  demandName: string;
  roleName?: string;
  headcount: number;
  startYear: number;
  startWeek: number;
  endYear: number;
  endWeek: number;
  weekStart?: number;
  weekEnd?: number;
  hoursPerWeekPerPerson: number;
  hoursPerWeek?: number;
  skillRequirement?: string;
  requiredSkill?: string;
}

export async function createScenario(payload: CreateScenarioPayload): Promise<ScenarioResult> {
  const body = {
    code: payload.code,
    name: payload.name,
    description: payload.description,
    orgUnitId: payload.orgUnitId,
    fromYear: payload.fromYear ?? payload.startYear,
    fromWeek: payload.fromWeek ?? payload.startWeek,
    durationWeeks: payload.durationWeeks,
  };
  return apiRequest<ScenarioResult>("/resource-scenarios", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function listScenarios(orgUnitId?: number): Promise<ScenarioResult[]> {
  const query = orgUnitId ? `?orgUnitId=${orgUnitId}` : "";
  return apiRequest<ScenarioResult[]>(`/resource-scenarios${query}`);
}

export async function getScenarioById(id: number): Promise<ScenarioDetailResult> {
  return apiRequest<ScenarioDetailResult>(`/resource-scenarios/${id}`);
}

export async function addScenarioDemand(
  scenarioId: number,
  payload: AddDemandPayload
): Promise<ScenarioDemandResult> {
  return apiRequest<ScenarioDemandResult>(`/resource-scenarios/${scenarioId}/demands`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateScenarioDemand(
  scenarioId: number,
  demandId: number,
  payload: UpdateDemandPayload
): Promise<ScenarioDemandResult> {
  return apiRequest<ScenarioDemandResult>(`/resource-scenarios/${scenarioId}/demands/${demandId}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function deleteScenarioDemand(
  scenarioId: number,
  demandId: number
): Promise<void> {
  return apiRequest<void>(`/resource-scenarios/${scenarioId}/demands/${demandId}`, {
    method: "DELETE",
  });
}

export async function getScenarioSimulation(id: number): Promise<ScenarioSimulationResult> {
  return apiRequest<ScenarioSimulationResult>(`/resource-scenarios/${id}/simulation`);
}

// ============================================================================
// NCL-08-CN-003: Áp dụng kịch bản vào phân bổ thật (Apply Scenario To Real Allocations)
// ============================================================================

export interface WeeklyComparisonCellResult {
  year: number;
  weekNumber: number;
  currentProjectHours: number;
  currentTotalAllocatedHours: number;
  scenarioAdditionalHours: number;
  newProjectHours: number;
  newTotalAllocatedHours: number;
  availableHours: number;
  isOverloaded: boolean;
}

export interface EmployeeComparisonRowResult {
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  professionalRole: string;
  weeklyCells: WeeklyComparisonCellResult[];
  totalScenarioHours: number;
}

export interface WeeklyHeaderResult {
  year: number;
  weekNumber: number;
  weekLabel: string;
}

export interface ApplyScenarioPreviewResult {
  scenarioId: number;
  scenarioCode: string;
  scenarioName: string;
  targetProjectId: number;
  targetProjectName: string;
  isBaselineStale: boolean;
  staleReasons: string[];
  weeks: WeeklyHeaderResult[];
  employeeComparisons: EmployeeComparisonRowResult[];
  affectedEmployeesCount: number;
  totalAdditionalHours: number;
}

export interface ApplyScenarioPayload {
  targetProjectId: number;
  note?: string;
}

export interface ApplyScenarioResult {
  scenarioId: number;
  scenarioCode: string;
  targetProjectId: number;
  targetProjectName: string;
  status: string;
  appliedAllocationsCount: number;
  affectedEmployeesCount: number;
  appliedAt: string;
  message: string;
}

export async function getScenarioApplyPreview(
  scenarioId: number,
  targetProjectId: number
): Promise<ApplyScenarioPreviewResult> {
  return apiRequest<ApplyScenarioPreviewResult>(
    `/resource-scenarios/${scenarioId}/apply-preview?targetProjectId=${targetProjectId}`
  );
}

export async function applyScenarioToRealAllocations(
  scenarioId: number,
  payload: ApplyScenarioPayload
): Promise<ApplyScenarioResult> {
  return apiRequest<ApplyScenarioResult>(`/resource-scenarios/${scenarioId}/apply`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function refreshScenarioBaseline(scenarioId: number): Promise<ScenarioResult> {
  return apiRequest<ScenarioResult>(`/resource-scenarios/${scenarioId}/refresh-baseline`, {
    method: "POST",
  });
}

// ============================================================================
// NCL.08.CN.004: So sánh đa kịch bản (Scenario Comparison)
// ============================================================================

export interface CompareScenariosPayload {
  scenarioIds: number[];
}

export interface OverloadedEmployeeSummaryResult {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  professionalRole: string;
  overloadedWeeksCount: number;
  maxExcessHours: number;
  peakUtilizationPercentage: number;
}

export interface ScenarioComparisonItemResult {
  scenarioId: number;
  scenarioCode: string;
  scenarioName: string;
  description: string | null;
  orgUnitId: number;
  orgUnitName: string;
  status: string;
  fromYear: number;
  fromWeek: number;
  durationWeeks: number;
  overloadedEmployeesCount: number;
  totalShortfallHours: number;
  totalRequiredAdditionalHours: number;
  totalDemandHours: number;
  totalWorkloadHours: number;
  totalAvailableHours: number;
  averageUtilizationPercentage: number;
  peakUtilizationPercentage: number;
  weeklyMetrics: WeeklySimulationMetricResult[];
  overloadedEmployees: OverloadedEmployeeSummaryResult[];
}

export interface ScenarioComparisonResult {
  scenarios: ScenarioComparisonItemResult[];
  isTimeframeAligned: boolean;
  isOrgUnitAligned: boolean;
  comparedAt: string;
}

export async function compareScenarios(
  payload: CompareScenariosPayload
): Promise<ScenarioComparisonResult> {
  return apiRequest<ScenarioComparisonResult>("/resource-scenarios/compare", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

// ============================================================================
// NCL.08.CN.006: Lưu & Chia sẻ kịch bản (Save & Share Scenario)
// ============================================================================

export interface PatchScenarioPayload {
  name?: string;
  note?: string;
}

export interface ShareCandidateResult {
  userId: number;
  username: string;
  fullName: string;
  roleCode: string;
  roleName: string;
  orgUnitId: number | null;
  orgUnitName: string | null;
  managedProjectIds?: number[];
  managedProjectNames: string[];
}

export interface ScenarioShareResult {
  id: number;
  scenarioId: number;
  userId: number;
  sharedWithUserId?: number;
  username: string;
  sharedWithUsername?: string;
  fullName: string;
  sharedWithFullName?: string;
  roleCode: string;
  sharedWithRoleCode?: string;
  roleName?: string;
  sharedWithRoleName?: string;
  permission: string;
  accessLevel?: string;
  sharedBy: number;
  sharedByUserId?: number;
  sharedByName: string;
  sharedAt: string;
  createdAt?: string;
  revokedAt: string | null;
  isActive: boolean;
  active?: boolean;
}

export interface ShareScenarioPayload {
  userIds?: number[];
  recipientUserIds?: number[];
}

function normalizeShareResult(item: any): ScenarioShareResult {
  const userId = item.userId ?? item.sharedWithUserId;
  const username = item.username ?? item.sharedWithUsername ?? "";
  const fullName = item.fullName ?? item.sharedWithFullName ?? username;
  const roleCode = item.roleCode ?? item.sharedWithRoleCode ?? "";
  const roleName = item.roleName ?? item.sharedWithRoleName ?? "";
  const permission = item.permission ?? item.accessLevel ?? "VIEW_ONLY";
  const sharedBy = item.sharedBy ?? item.sharedByUserId;
  const sharedByName = item.sharedByName ?? (sharedBy ? `User #${sharedBy}` : "--");
  const sharedAt = item.sharedAt ?? item.createdAt ?? "";
  const isActive = item.isActive !== undefined ? Boolean(item.isActive) : (item.active !== undefined ? Boolean(item.active) : true);

  return {
    ...item,
    userId,
    sharedWithUserId: userId,
    username,
    sharedWithUsername: username,
    fullName,
    sharedWithFullName: fullName,
    roleCode,
    sharedWithRoleCode: roleCode,
    roleName,
    sharedWithRoleName: roleName,
    permission,
    accessLevel: permission,
    sharedBy,
    sharedByUserId: sharedBy,
    sharedByName,
    sharedAt,
    createdAt: sharedAt,
    revokedAt: item.revokedAt ?? null,
    isActive,
    active: isActive,
  };
}

export async function patchScenario(
  id: number,
  payload: PatchScenarioPayload
): Promise<ScenarioResult> {
  return apiRequest<ScenarioResult>(`/resource-scenarios/${id}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export async function saveScenario(id: number): Promise<ScenarioResult> {
  return apiRequest<ScenarioResult>(`/resource-scenarios/${id}/save`, {
    method: "POST",
  });
}

export async function getShareCandidates(
  id: number,
  query?: string
): Promise<ShareCandidateResult[]> {
  const q = query ? `?query=${encodeURIComponent(query)}` : "";
  return apiRequest<ShareCandidateResult[]>(`/resource-scenarios/${id}/share-candidates${q}`);
}

export async function shareScenario(
  id: number,
  payload: ShareScenarioPayload
): Promise<ScenarioShareResult[]> {
  const ids = payload.userIds ?? payload.recipientUserIds ?? [];
  const res = await apiRequest<ScenarioShareResult[]>(`/resource-scenarios/${id}/shares`, {
    method: "POST",
    body: JSON.stringify({
      userIds: ids,
      recipientUserIds: ids,
    }),
  });
  return (res || []).map(normalizeShareResult);
}

export async function unshareScenario(id: number, userId: number): Promise<void> {
  return apiRequest<void>(`/resource-scenarios/${id}/shares/${userId}`, {
    method: "DELETE",
  });
}

export async function getScenarioShares(id: number): Promise<ScenarioShareResult[]> {
  const res = await apiRequest<ScenarioShareResult[]>(`/resource-scenarios/${id}/shares`);
  return (res || []).map(normalizeShareResult);
}
