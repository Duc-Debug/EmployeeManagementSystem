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
  startYear: number;
  startWeek: number;
  durationWeeks: number;
  baseSnapshotAt: string;
  createdBy: number;
  creatorName: string;
  createdAt: string;
  appliedAt: string | null;
  demandCount: number;
  totalSnapshotEmployees: number;
}

export interface ScenarioDemandResult {
  id: number;
  scenarioId: number;
  roleName: string;
  headcount: number;
  weekStart: number;
  weekEnd: number;
  hoursPerWeek: number;
  requiredSkill: string | null;
  createdAt: string;
}

export interface ScenarioDetailResult {
  scenario: ScenarioResult;
  demands: ScenarioDemandResult[];
}

export interface WeeklySimulationMetricResult {
  year: number;
  weekNumber: number;
  snapshotAllocatedHours: number;
  demandHours: number;
  totalWorkloadHours: number;
  availableCapacityHours: number;
  utilizationPercentage: number;
  status: "OVERLOADED" | "OPTIMAL" | "AVAILABLE" | "UNDERLOADED" | string;
}

export interface EmployeeSnapshotCellResult {
  year: number;
  weekNumber: number;
  snapshotAllocatedHours: number;
  snapshotAvailableHours: number;
}

export interface EmployeeSnapshotRowResult {
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  professionalRole: string;
  weeklyCells: EmployeeSnapshotCellResult[];
}

export interface ScenarioSimulationResult {
  scenarioId: number;
  weeklyMetrics: WeeklySimulationMetricResult[];
  employeeRows: EmployeeSnapshotRowResult[];
  overloadThreshold: number;
  idleThreshold: number;
}

export interface CreateScenarioPayload {
  code: string;
  name: string;
  description?: string;
  orgUnitId: number;
  startYear: number;
  startWeek: number;
  durationWeeks: number;
}

export interface AddDemandPayload {
  roleName: string;
  headcount: number;
  weekStart: number;
  weekEnd: number;
  hoursPerWeek: number;
  requiredSkill?: string;
}

export interface UpdateDemandPayload {
  roleName: string;
  headcount: number;
  weekStart: number;
  weekEnd: number;
  hoursPerWeek: number;
  requiredSkill?: string;
}

export async function createScenario(payload: CreateScenarioPayload): Promise<ScenarioResult> {
  return apiRequest<ScenarioResult>("/resource-scenarios", {
    method: "POST",
    body: JSON.stringify(payload),
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
