"use client";

import { apiRequest } from "../api-client";

export type UnavailabilityReasonType = "TRAINING" | "BUSINESS_TRIP" | "PERSONAL" | "OTHER";

export type UnavailabilityStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export interface UnavailabilityDeclarationResult {
  id: number;
  employeeId: number;
  startDate: string; // YYYY-MM-DD
  endDate: string; // YYYY-MM-DD
  reasonType: UnavailabilityReasonType;
  reasonDetail?: string | null;
  totalHoursDeducted: number;
  status: UnavailabilityStatus;
  approverId?: number | null;
  approverComment?: string | null;
  approvedAt?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface SubmitUnavailabilityRequest {
  employeeId: number;
  startDate: string;
  endDate: string;
  reasonType: UnavailabilityReasonType;
  reasonDetail?: string;
}

export interface UnavailabilityConflictDetail {
  allocationId: number;
  projectId: number;
  year: number;
  weekNumber: number;
  allocatedHours: number;
}

export interface UnavailabilityConflictCheckResult {
  hasConflict: boolean;
  conflictingAllocationsCount: number;
  totalConflictingHours: number;
  conflictingAllocations: UnavailabilityConflictDetail[];
  warningMessage?: string | null;
}

export interface ApproveUnavailabilityRequest {
  approverComment?: string;
  confirmConflictWarning?: boolean;
}

export interface RejectUnavailabilityRequest {
  rejectReason?: string;
}

export const UNAVAILABILITY_REASON_LABELS: Record<UnavailabilityReasonType, string> = {
  TRAINING: "Đào tạo chuyên môn",
  BUSINESS_TRIP: "Đi công tác",
  PERSONAL: "Việc cá nhân",
  OTHER: "Lý do khác",
};

export const UNAVAILABILITY_STATUS_LABELS: Record<UnavailabilityStatus, string> = {
  PENDING: "Chờ phê duyệt",
  APPROVED: "Đã phê duyệt",
  REJECTED: "Đã từ chối",
  CANCELLED: "Đã hủy",
};

/**
 * NCL-13-CN-003: Nộp khai báo thời gian không sẵn sàng.
 */
export async function submitUnavailability(
  payload: SubmitUnavailabilityRequest
): Promise<UnavailabilityDeclarationResult> {
  return await apiRequest<UnavailabilityDeclarationResult>("/unavailability-declarations", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/**
 * Lấy danh sách khai báo cá nhân của nhân sự đang đăng nhập.
 */
export async function getMyUnavailabilityDeclarations(): Promise<UnavailabilityDeclarationResult[]> {
  return await apiRequest<UnavailabilityDeclarationResult[]>("/unavailability-declarations/me", {
    method: "GET",
  });
}

/**
 * Lấy danh sách khai báo đang chờ duyệt theo phạm vi phân quyền.
 */
export async function getPendingUnavailabilityDeclarations(
  orgUnitId?: number | null
): Promise<UnavailabilityDeclarationResult[]> {
  const query = orgUnitId != null ? `?orgUnitId=${orgUnitId}` : "";
  return await apiRequest<UnavailabilityDeclarationResult[]>(
    `/unavailability-declarations/pending${query}`,
    {
      method: "GET",
    }
  );
}

/**
 * Kiểm tra xung đột trước khi phê duyệt khai báo (TC-02).
 */
export async function checkUnavailabilityConflict(
  id: number
): Promise<UnavailabilityConflictCheckResult> {
  return await apiRequest<UnavailabilityConflictCheckResult>(
    `/unavailability-declarations/${id}/check-conflict`,
    {
      method: "GET",
    }
  );
}

/**
 * Phê duyệt khai báo thời gian không sẵn sàng (TC-01, TC-02, QTN-24).
 */
export async function approveUnavailability(
  id: number,
  payload: ApproveUnavailabilityRequest
): Promise<UnavailabilityDeclarationResult> {
  return await apiRequest<UnavailabilityDeclarationResult>(
    `/unavailability-declarations/${id}/approve`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  );
}

/**
 * Từ chối khai báo thời gian không sẵn sàng kèm lý do.
 */
export async function rejectUnavailability(
  id: number,
  payload: RejectUnavailabilityRequest
): Promise<UnavailabilityDeclarationResult> {
  return await apiRequest<UnavailabilityDeclarationResult>(
    `/unavailability-declarations/${id}/reject`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  );
}

/**
 * Hủy khai báo thời gian không sẵn sàng (hoàn trả capacity tuần nếu đã duyệt).
 */
export async function cancelUnavailability(
  id: number
): Promise<UnavailabilityDeclarationResult> {
  return await apiRequest<UnavailabilityDeclarationResult>(
    `/unavailability-declarations/${id}`,
    {
      method: "DELETE",
    }
  );
}