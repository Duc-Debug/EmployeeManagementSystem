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
 * NCL-05-CN-005: DTO Thống kê số ngày phép của nhân viên
 */
export interface LeaveBalanceDto {
  employeeId: number;
  year: number;
  entitledDays: number;
  carriedOverDays: number;
  totalAllocatedDays: number;
  usedDays: number;
  approvedDays?: number;
  pendingDays: number;
  remainingDays: number;
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
 * NCL-05-CN-005: Lấy thông tin số ngày phép còn lại của nhân viên đang đăng nhập (TC-01, TC-04)
 */
export async function getMyLeaveBalance(year?: number): Promise<LeaveBalanceDto> {
  const query = year ? `?year=${year}` : "";
  return apiRequest<LeaveBalanceDto>(`/leaves/balances/me${query}`);
}

/**
 * NCL-05-CN-005: Quản lý hoặc HR xem số ngày phép còn lại của một nhân viên bất kỳ (TC-03)
 */
export async function getEmployeeLeaveBalance(
  employeeId: number | string,
  year?: number
): Promise<LeaveBalanceDto> {
  const numericId = typeof employeeId === "string" ? employeeId.replace(/\D/g, "") : employeeId;
  const query = year ? `?year=${year}` : "";
  return apiRequest<LeaveBalanceDto>(`/leaves/balances/employees/${numericId}${query}`);
}
