"use client";

import { apiRequest } from "../api-client";

export type AllocationPeriodType = "MONTH" | "QUARTER" | "YEAR";
export type AllocationPeriodStatus = "OPEN" | "LOCKED";

export interface AllocationPlanSnapshotItemResult {
  id: number;
  snapshotId: number;
  projectId: number;
  projectCode: string;
  projectName: string;
  employeeId: number;
  employeeCode: string;
  employeeFullName: string;
  year: number;
  weekNumber: number;
  allocatedHours: number;
}

export interface AllocationPlanSnapshotResult {
  id: number;
  periodId: number;
  snapshotVersion: number;
  totalAllocations: number;
  totalAllocatedHours: number;
  lockedBy: number;
  lockedByName?: string;
  lockedAt: string;
  items: AllocationPlanSnapshotItemResult[];
}

export interface AllocationPeriodResult {
  id: number;
  name: string;
  periodType: AllocationPeriodType;
  year: number;
  startWeek: number;
  endWeek: number;
  status: AllocationPeriodStatus;
  lockedBy?: number | null;
  lockedByName?: string | null;
  lockedAt?: string | null;
  unlockedBy?: number | null;
  unlockedByName?: string | null;
  unlockedAt?: string | null;
  unlockReason?: string | null;
  createdBy: number;
  createdAt: string;
  latestSnapshot?: AllocationPlanSnapshotResult | null;
}

export interface CreatePeriodPayload {
  name: string;
  periodType: AllocationPeriodType;
  year: number;
  startWeek: number;
  endWeek: number;
}

export interface UnlockPeriodPayload {
  reason: string;
}

export interface PeriodLockCheckResult {
  isLocked: boolean;
  periodId?: number | null;
  periodName?: string | null;
  year: number;
  weekNumber: number;
  message: string;
}

/**
 * Lấy danh sách kỳ kế hoạch phân bổ (lọc theo năm và trạng thái)
 */
export async function getAllocationPeriods(params?: {
  year?: number;
  status?: AllocationPeriodStatus;
}): Promise<AllocationPeriodResult[]> {
  const searchParams = new URLSearchParams();
  if (params?.year != null) searchParams.append("year", String(params.year));
  if (params?.status != null) searchParams.append("status", params.status);

  const queryStr = searchParams.toString();
  return apiRequest<AllocationPeriodResult[]>(
    `/allocations/periods${queryStr ? `?${queryStr}` : ""}`
  );
}

/**
 * Lấy thông tin chi tiết một kỳ kế hoạch phân bổ
 */
export async function getPeriodById(id: number): Promise<AllocationPeriodResult> {
  return apiRequest<AllocationPeriodResult>(`/allocations/periods/${id}`);
}

/**
 * Tạo kỳ kế hoạch phân bổ mới
 */
export async function createAllocationPeriod(
  payload: CreatePeriodPayload
): Promise<AllocationPeriodResult> {
  return apiRequest<AllocationPeriodResult>("/allocations/periods", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/**
 * [TC-01, TC-03]: Khóa kỳ kế hoạch phân bổ và lưu bản chụp kế hoạch
 */
export async function lockAllocationPeriod(id: number): Promise<AllocationPeriodResult> {
  return apiRequest<AllocationPeriodResult>(`/allocations/periods/${id}/lock`, {
    method: "POST",
  });
}

/**
 * [TC-04]: Mở lại kỳ kế hoạch phân bổ kèm theo lý do bắt buộc
 */
export async function unlockAllocationPeriod(
  id: number,
  payload: UnlockPeriodPayload
): Promise<AllocationPeriodResult> {
  return apiRequest<AllocationPeriodResult>(`/allocations/periods/${id}/unlock`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/**
 * Lấy danh sách các bản chụp kế hoạch (Snapshots) của kỳ
 */
export async function getPeriodSnapshots(
  periodId: number
): Promise<AllocationPlanSnapshotResult[]> {
  return apiRequest<AllocationPlanSnapshotResult[]>(
    `/allocations/periods/${periodId}/snapshots`
  );
}

/**
 * Lấy chi tiết nội dung phân bổ trong một bản chụp kế hoạch cụ thể
 */
export async function getSnapshotDetail(
  periodId: number,
  snapshotId: number
): Promise<AllocationPlanSnapshotResult> {
  return apiRequest<AllocationPlanSnapshotResult>(
    `/allocations/periods/${periodId}/snapshots/${snapshotId}`
  );
}

/**
 * Kiểm tra nhanh tuần cụ thể có đang bị khóa bởi kỳ nào hay không
 */
export async function checkWeekLock(
  year: number,
  weekNumber: number
): Promise<PeriodLockCheckResult> {
  const query = new URLSearchParams({
    year: String(year),
    weekNumber: String(weekNumber),
  });
  return apiRequest<PeriodLockCheckResult>(
    `/allocations/periods/check-lock?${query.toString()}`
  );
}
