"use client";

import { apiRequest } from "../api-client";

export interface ScenarioResult {
  id: number;
  code: string;
  name: string;
  description: string | null;
  orgUnitId: number;
  orgUnitName: string;
  status: "draft" | "applied" | "discarded" | string;
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

