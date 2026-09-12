"use client";

import { apiRequest } from "../api-client";

export interface AllocateResourcePayload {
  employeeId: number;
  projectId: number;
  year: number;
  weekNumber: number;
  allocatedHours?: number;
  allocationPercentage?: number;
}

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

/**
 * Phân bổ nhân sự vào dự án theo tuần (NCL-06-CN-001)
 */
export async function allocateResource(payload: AllocateResourcePayload): Promise<WeeklyCapacityResult> {
  return await apiRequest<WeeklyCapacityResult>("/allocations", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/**
 * Lấy danh sách công suất và số giờ còn rảnh của nhân sự theo tuần
 */
export async function getWeeklyCapacities(
  employeeIds: number[],
  year: number,
  weekNumber: number
): Promise<WeeklyCapacityResult[]> {
  if (!employeeIds || employeeIds.length === 0) return [];
  const idsParam = employeeIds.join(",");
  return await apiRequest<WeeklyCapacityResult[]>(
    `/allocations/capacity?employeeIds=${idsParam}&year=${year}&weekNumber=${weekNumber}`
  );
}

