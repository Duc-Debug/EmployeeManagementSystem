"use client";

import { apiRequest } from "../api-client";

export interface ProjectWorkloadAllocationResult {
  projectId: number;
  projectName: string;
  projectCode: string;
  projectRoleId: number | null;
  projectRoleName: string | null;
  allocatedHours: number;
  allocationPercentage: number;
}

export interface WeeklyWorkloadItemResult {
  year: number;
  weekNumber: number;
  startDate: string;
  endDate: string;
  weekLabel: string;
  standardHours: number;
  holidayHours: number;
  approvedLeaveHours: number;
  netAvailableHours: number;
  totalAllocatedHours: number;
  utilizationPercentage: number;
  status: "OVERLOADED" | "NORMAL" | "IDLE" | string;
  overloadHours: number;
  projectAllocations: ProjectWorkloadAllocationResult[];
}

export interface WorkloadSummaryResult {
  totalStandardHours: number;
  totalHolidayHours: number;
  totalApprovedLeaveHours: number;
  totalNetAvailableHours: number;
  totalAllocatedHours: number;
  averageUtilizationPercentage: number;
  overloadedWeeksCount: number;
  normalWeeksCount: number;
  idleWeeksCount: number;
  maxUtilizationPercentage: number;
  maxUtilizationWeek: string;
}

export interface UpcomingWorkloadResult {
  employeeId: number;
  employeeCode: string;
  employeeName: string;
  orgUnitId: number | null;
  orgUnitName: string;
  fromYear: number;
  fromWeek: number;
  durationWeeks: number;
  effectiveOverloadThreshold: number;
  effectiveIdleThreshold: number;
  weeklyWorkloads: WeeklyWorkloadItemResult[];
  summary: WorkloadSummaryResult;
  generatedAt: string;
}

/**
 * NCL-13-CN-004: Lấy khối lượng công việc 8 tuần sắp tới của chính tôi.
 */
export async function getMyUpcomingWorkload(
  fromYear?: number,
  fromWeek?: number,
  durationWeeks: number = 8
): Promise<UpcomingWorkloadResult> {
  const params = new URLSearchParams();
  if (fromYear !== undefined && fromYear !== null) {
    params.append("fromYear", fromYear.toString());
  }
  if (fromWeek !== undefined && fromWeek !== null) {
    params.append("fromWeek", fromWeek.toString());
  }
  if (durationWeeks) {
    params.append("durationWeeks", durationWeeks.toString());
  }

  const query = params.toString() ? `?${params.toString()}` : "";
  return await apiRequest<UpcomingWorkloadResult>(`/workload/my-upcoming${query}`, {
    method: "GET",
  });
}

/**
 * NCL-13-CN-004: Lấy khối lượng công việc sắp tới của một nhân viên (DataScope validation).
 */
export async function getEmployeeUpcomingWorkload(
  employeeId: number,
  fromYear?: number,
  fromWeek?: number,
  durationWeeks: number = 8
): Promise<UpcomingWorkloadResult> {
  const params = new URLSearchParams();
  if (fromYear !== undefined && fromYear !== null) {
    params.append("fromYear", fromYear.toString());
  }
  if (fromWeek !== undefined && fromWeek !== null) {
    params.append("fromWeek", fromWeek.toString());
  }
  if (durationWeeks) {
    params.append("durationWeeks", durationWeeks.toString());
  }

  const query = params.toString() ? `?${params.toString()}` : "";
  return await apiRequest<UpcomingWorkloadResult>(`/workload/employee/${employeeId}${query}`, {
    method: "GET",
  });
}
