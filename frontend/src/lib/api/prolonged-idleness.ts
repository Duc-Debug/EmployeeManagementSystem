"use client";

import { apiRequest } from "../api-client";

export interface WeeklyIdlenessDetail {
  year: number;
  weekNumber: number;
  availableHours: number;
  allocatedHours: number;
  emptyHours: number;
  utilizationPercentage: number;
  isUnderutilized: boolean;
  isFullLeaveWeek: boolean;
}

export interface ProlongedIdleStaffItem {
  employeeId: number;
  employeeCode: string;
  fullName: string;
  orgUnitId: number | null;
  departmentName: string;
  positionTitle: string;
  consecutiveIdleWeeks: number;
  totalEmptyHours: number;
  averageUtilization: number;
  weeklyDetails: WeeklyIdlenessDetail[];
}

export interface ProlongedIdlenessReportResult {
  orgUnitId: number | null;
  orgUnitName: string;
  fromYear: number;
  fromWeek: number;
  durationWeeks: number;
  effectiveIdleThreshold: number;
  consecutiveThreshold: number;
  totalIdleEmployees: number;
  page: number;
  size: number;
  totalPages: number;
  items: ProlongedIdleStaffItem[];
}

export interface ProlongedIdlenessQueryParams {
  orgUnitId?: number | null;
  fromYear?: number | null;
  fromWeek?: number | null;
  durationWeeks?: number | null;
  consecutiveThreshold?: number | null;
  search?: string | null;
  page?: number | null;
  size?: number | null;
}

export interface AcknowledgeProlongedIdleStaffPayload {
  employeeId: number;
  actionTaken: string;
  notes?: string | null;
}

export interface AcknowledgeProlongedIdleStaffResult {
  employeeId: number;
  employeeName: string;
  actionTaken: string;
  notes: string | null;
  acknowledgedBy: number;
  acknowledgedAt: string;
  status: string;
}

/**
 * Lấy danh sách nhân sự bị cảnh báo nhàn rỗi kéo dài (NCL-07-CN-006 / QTN-23)
 */
export async function getProlongedIdleStaff(
  params?: ProlongedIdlenessQueryParams
): Promise<ProlongedIdlenessReportResult> {
  const query = new URLSearchParams();

  if (params?.orgUnitId !== undefined && params.orgUnitId !== null) {
    query.append("orgUnitId", String(params.orgUnitId));
  }
  if (params?.fromYear !== undefined && params.fromYear !== null) {
    query.append("fromYear", String(params.fromYear));
  }
  if (params?.fromWeek !== undefined && params.fromWeek !== null) {
    query.append("fromWeek", String(params.fromWeek));
  }
  if (params?.durationWeeks !== undefined && params.durationWeeks !== null) {
    query.append("durationWeeks", String(params.durationWeeks));
  }
  if (params?.consecutiveThreshold !== undefined && params.consecutiveThreshold !== null) {
    query.append("consecutiveThreshold", String(params.consecutiveThreshold));
  }
  if (params?.search && params.search.trim()) {
    query.append("search", params.search.trim());
  }
  if (params?.page !== undefined && params.page !== null) {
    query.append("page", String(params.page));
  }
  if (params?.size !== undefined && params.size !== null) {
    query.append("size", String(params.size));
  }

  const queryString = query.toString();
  const endpoint = `/capacity/prolonged-idleness${queryString ? `?${queryString}` : ""}`;
  return apiRequest<ProlongedIdlenessReportResult>(endpoint);
}

/**
 * Xác nhận xử lý cảnh báo nhân sự nhàn rỗi kéo dài (NCL-07-CN-006-TC-04)
 */
export async function acknowledgeProlongedIdleStaff(
  payload: AcknowledgeProlongedIdleStaffPayload
): Promise<AcknowledgeProlongedIdleStaffResult> {
  return apiRequest<AcknowledgeProlongedIdleStaffResult>(
    "/capacity/prolonged-idleness/acknowledge",
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  );
}
