"use client";

import { apiRequest } from "../api-client";

export interface WeeklyCapacityResult {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  year: number;
  weekNumber: number;
  standardHours: number;
  netAvailableHours: number;
  totalAllocatedHours: number;
  remainingAvailableHours: number;
  isOverAllocated: boolean;
  warningMessage?: string;
}

export async function getWeeklyCapacities(
  employeeIds: readonly number[],
  year: number,
  weekNumber: number,
): Promise<WeeklyCapacityResult[]> {
  const params = new URLSearchParams({ year: String(year), weekNumber: String(weekNumber) });
  employeeIds.forEach((id) => params.append("employeeIds", String(id)));
  return apiRequest<WeeklyCapacityResult[]>(`/allocations/capacity?${params.toString()}`);
}

export interface ProjectWeeklyAllocationResult {
  employeeId: number;
  projectId: number;
  year: number;
  weekNumber: number;
  allocatedHours: number;
}

export async function getProjectWeeklyAllocations(
  projectId: number,
  year: number,
  startWeek: number,
  endWeek: number,
): Promise<ProjectWeeklyAllocationResult[]> {
  const params = new URLSearchParams({
    year: String(year),
    startWeek: String(startWeek),
    endWeek: String(endWeek),
  });
  return apiRequest<ProjectWeeklyAllocationResult[]>(`/projects/${projectId}/allocations?${params.toString()}`);
}

export async function allocateProjectHours(payload: {
  employeeId: number;
  projectId: number;
  year: number;
  weekNumber: number;
  allocatedHours: number;
}): Promise<WeeklyCapacityResult> {
  return apiRequest<WeeklyCapacityResult>('/allocations', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
