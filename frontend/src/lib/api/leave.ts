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
