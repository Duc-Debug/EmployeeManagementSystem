"use client";

import { apiRequest } from "../api-client";

export interface DeclareWeeklyAvailabilityPayload {
  year: number;
  weekNumber: number;
  standardHours: number;
}

export interface WeeklyAvailabilityResult {
  employeeId: number;
  year: number;
  weekNumber: number;
  standardHours: number;
  holidayHours: number;
  approvedLeaveHours: number;
  netAvailableHours: number;
  version?: number;
}

/**
 * NCL-02-CN-003: Khai báo số giờ chuẩn một tuần cho nhân sự.
 * Roles: HR (VT-05), Admin (VT-06).
 */
export async function declareWeeklyAvailability(
  employeeId: number,
  payload: DeclareWeeklyAvailabilityPayload
): Promise<WeeklyAvailabilityResult> {
  return await apiRequest<WeeklyAvailabilityResult>(`/employees/${employeeId}/availability`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

/**
 * Tính toán & xem năng lực khả dụng theo tuần theo quy tắc QTN-10:
 * Khả dụng = Giờ chuẩn - Giờ lễ - Giờ nghỉ phép đã duyệt.
 * Roles: RM (VT-03), HR (VT-05), Admin (VT-06).
 */
export async function getWeeklyCapacity(
  employeeId: number,
  year: number,
  weekNumber: number
): Promise<WeeklyAvailabilityResult> {
  return await apiRequest<WeeklyAvailabilityResult>(
    `/employees/${employeeId}/capacity?year=${year}&weekNumber=${weekNumber}`,
    {
      method: "GET",
    }
  );
}

/**
 * Lấy thông tin giờ chuẩn / khả dụng tuần đã lưu của nhân sự.
 */
export async function getWeeklyAvailability(
  employeeId: number,
  year: number,
  weekNumber: number
): Promise<WeeklyAvailabilityResult> {
  return await apiRequest<WeeklyAvailabilityResult>(
    `/employees/${employeeId}/availability?year=${year}&weekNumber=${weekNumber}`,
    {
      method: "GET",
    }
  );
}
