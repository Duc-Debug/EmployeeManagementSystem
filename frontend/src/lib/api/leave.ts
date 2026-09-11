"use client";

import { apiRequest } from "../api-client";

export interface LeaveRequestDto {
  id: number;
  employeeId: number;
  employeeName?: string;
  department?: string;
  leaveType: "ANNUAL" | "UNPAID" | "SICK" | "PERSONAL";
  startDate: string;
  endDate: string;
  daysCount: number;
  hoursDeducted: number;
  reason?: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
  approverId?: number;
  approverComment?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ProjectAllocationImpactDto {
  projectId: number;
  projectName: string;
  year: number;
  weekNumber: number;
  allocatedHours: number;
}

export interface LeaveImpactDto {
  leaveRequestId: number;
  employeeId: number;
  employeeName: string;
  startDate: string;
  endDate: string;
  daysCount: number;
  hoursDeducted: number;
  totalAllocatedHoursInLeavePeriod: number;
  hasConflict: boolean;
  affectedProjects: ProjectAllocationImpactDto[];
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
 * NCL-05-CN-003: Lấy danh sách đơn chờ duyệt (RM / HR)
 */
export async function getPendingLeaveRequests(): Promise<LeaveRequestDto[]> {
  return apiRequest<LeaveRequestDto[]>("/leave-requests/pending");
}

/**
 * NCL-05-CN-003 (TC-02): Lấy thông tin cảnh báo tác động dự án của đơn nghỉ phép
 */
export async function getLeaveImpact(id: number | string): Promise<LeaveImpactDto> {
  const numericId = typeof id === "string" ? id.replace(/\D/g, "") : id;
  return apiRequest<LeaveImpactDto>(`/leave-requests/${numericId}/impact`);
}

/**
 * NCL-05-CN-003 (TC-01 & QTN-10): Duyệt đơn nghỉ phép
 */
export async function approveLeaveRequest(id: number | string, comment?: string): Promise<LeaveRequestDto> {
  const numericId = typeof id === "string" ? id.replace(/\D/g, "") : id;
  return apiRequest<LeaveRequestDto>(`/leave-requests/${numericId}/approve`, {
    method: "PUT",
    body: JSON.stringify({ comment: comment || "" }),
  });
}

/**
 * NCL-05-CN-003 (TC-04): Từ chối đơn nghỉ phép kèm lý do bắt buộc
 */
export async function rejectLeaveRequest(id: number | string, reason: string): Promise<LeaveRequestDto> {
  const numericId = typeof id === "string" ? id.replace(/\D/g, "") : id;
  return apiRequest<LeaveRequestDto>(`/leave-requests/${numericId}/reject`, {
    method: "PUT",
    body: JSON.stringify({ reason }),
  });
}
