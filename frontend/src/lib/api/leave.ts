"use client";

import { apiRequest } from "../api-client";

export interface LeaveRequestDto {
  id: number;
  employeeId: number;
  leaveType: "ANNUAL" | "UNPAID" | "SICK" | "PERSONAL";
  startDate: string;
  endDate: string;
  daysCount: number;
  hoursDeducted: number;
  reason?: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
  createdAt: string;
  updatedAt?: string;
}

export interface SubmitLeaveRequestPayload {
  leaveType: "ANNUAL" | "UNPAID" | "SICK" | "PERSONAL";
  startDate: string;
  endDate: string;
  reason?: string;
}

/**
 * NCL-05-CN-002: Gửi đơn xin nghỉ phép mới (TC-01)
 */
export async function submitLeaveRequest(payload: SubmitLeaveRequestPayload): Promise<LeaveRequestDto> {
  return apiRequest<LeaveRequestDto>("/leave-requests", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/**
 * Lấy danh sách toàn bộ đơn nghỉ phép của nhân viên đang đăng nhập
 */
export async function getMyLeaveRequests(): Promise<LeaveRequestDto[]> {
  return apiRequest<LeaveRequestDto[]>("/leave-requests/my");
}

/**
 * Hủy đơn xin nghỉ phép cá nhân khi còn ở trạng thái PENDING
 */
export async function cancelLeaveRequest(id: number | string): Promise<void> {
  const numericId = typeof id === "string" ? id.replace(/\D/g, "") : id;
  await apiRequest<void>(`/leave-requests/${numericId}/cancel`, {
    method: "PUT",
  });
}

/**
 * NCL-05-CN-006: Lịch nghỉ của bộ phận theo tháng
 */
export interface LeaveCalendarItemDto {
  leaveRequestId: number;
  employeeId: number;
  employeeCode: string;
  fullName: string;
  startDate: string;
  endDate: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
  hoursDeducted: number;
  leaveType: "ANNUAL" | "UNPAID" | "SICK" | "PERSONAL";
  reason?: string;
}

export interface DailyLeaveSummaryDto {
  date: string;
  dayOfWeek: string;
  totalOnLeave: number;
  approvedCount: number;
  pendingCount: number;
  isWarning: boolean;
  warningMessage?: string | null;
  isWorkingDay: boolean;
  isHoliday: boolean;
  totalLeaveHours: number;
  leaveItems: LeaveCalendarItemDto[];
}

export interface DepartmentMonthlyLeaveCalendarDto {
  orgUnitId: number;
  orgUnitCode: string;
  orgUnitName: string;
  year: number;
  month: number;
  totalDepartmentEmployees: number;
  warningThresholdPercentage: number;
  totalLeaveRequests: number;
  warningDaysCount: number;
  leaveItems: LeaveCalendarItemDto[];
  dailySummaries: DailyLeaveSummaryDto[];
}

export interface GetDepartmentMonthlyLeaveCalendarParams {
  orgUnitId: number;
  year?: number;
  month?: number;
  warningThreshold?: number;
  includeSubUnits?: boolean;
}

/**
 * Tra cứu lịch nghỉ của bộ phận theo tháng (NCL-05-CN-006 & TC-01, TC-02, TC-03)
 */
export async function getDepartmentMonthlyLeaveCalendar(
  params: GetDepartmentMonthlyLeaveCalendarParams
): Promise<DepartmentMonthlyLeaveCalendarDto> {
  const searchParams = new URLSearchParams();
  searchParams.set("orgUnitId", String(params.orgUnitId));
  if (params.year != null) searchParams.set("year", String(params.year));
  if (params.month != null) searchParams.set("month", String(params.month));
  if (params.warningThreshold != null) searchParams.set("warningThreshold", String(params.warningThreshold));
  if (params.includeSubUnits != null) searchParams.set("includeSubUnits", String(params.includeSubUnits));

  return apiRequest<DepartmentMonthlyLeaveCalendarDto>(
    `/leave-requests/department-calendar?${searchParams.toString()}`
  );
}
