"use client";

import { apiRequest } from "../api-client";

export interface RoleEvaluationItem {
  roleId: number;
  roleCode: string;
  roleName: string;
  // Backward-compatibility aliases
  projectRoleId?: number;
  projectRoleCode?: string;
  projectRoleName?: string;
  originalShortfallHours: number;
  simulatedCapacityHours: number;
  remainingShortfallHours: number;
  simulatedEmployeesCount: number;
  suggestedRecruitsNeeded: number;
  isRoleBroken?: boolean;
}

export interface RecruitmentEvaluationResponse {
  scenarioId?: number;
  totalOriginalShortfallHours: number;
  totalSimulatedCapacityHours: number;
  totalRemainingShortfallHours: number;
  isPlanBroken: boolean;
  overloadedRoleCount: number;
  totalSimulatedEmployeesCount?: number;
  totalSuggestedRecruitsNeeded: number;
  roleEvaluations: RoleEvaluationItem[];
}

export interface SimulatedEmployeeResponse {
  id: number;
  scenarioId: number;
  candidateName: string;
  projectRoleId: number;
  projectRoleCode?: string;
  projectRoleName?: string;
  primarySkillId?: number | null;
  primarySkillName?: string | null;
  standardHoursPerWeek: number;
  weeksCount: number;
  totalCapacityHours: number;
  notes?: string | null;
  createdAt: string;
}

export interface CreateSimulatedEmployeePayload {
  candidateName: string;
  projectRoleId: number;
  primarySkillId?: number | null;
  standardHoursPerWeek: number;
  weeksCount: number;
  notes?: string;
}

export interface UpdateSimulatedEmployeePayload {
  candidateName: string;
  projectRoleId: number;
  primarySkillId?: number | null;
  standardHoursPerWeek: number;
  weeksCount: number;
  notes?: string;
}

/**
 * Tính tỷ lệ % bù đắp thiếu hụt công suất (0% - 100%)
 */
export function calculateCoveragePercentage(
  originalShortfall: number,
  simulatedCapacity: number
): number {
  if (!originalShortfall || originalShortfall <= 0) return 100;
  if (!simulatedCapacity || simulatedCapacity <= 0) return 0;
  const pct = (simulatedCapacity / originalShortfall) * 100;
  return Math.min(100, Math.round(pct * 10) / 10);
}

/**
 * Format số giờ đẹp mắt (VD: "40.0h", "120.5h")
 */
export function formatHoursDisplay(hours?: number | null): string {
  if (hours === undefined || hours === null) return "0h";
  return `${Number(hours).toLocaleString("vi-VN", {
    minimumFractionDigits: 1,
    maximumFractionDigits: 2,
  })}h`;
}

/**
 * Tính số FTE (Full-time Equivalent) tương đương dựa trên 40h/tuần
 */
export function calculateFteEquivalent(hoursPerWeek: number): string {
  if (!hoursPerWeek || hoursPerWeek <= 0) return "0.0 FTE";
  const fte = hoursPerWeek / 40;
  return `${fte.toFixed(1)} FTE`;
}

/**
 * Lấy tổng quan đánh giá tuyển dụng kịch bản (NCL-08-CN-005)
 */
export async function getRecruitmentEvaluation(
  scenarioId: number
): Promise<RecruitmentEvaluationResponse> {
  return await apiRequest<RecruitmentEvaluationResponse>(
    `/scenarios/${scenarioId}/recruitment-evaluation`
  );
}

/**
 * Lấy danh sách nhân sự giả định trong kịch bản (NCL-08-CN-005)
 */
export async function getSimulatedEmployees(
  scenarioId: number
): Promise<SimulatedEmployeeResponse[]> {
  const list = await apiRequest<SimulatedEmployeeResponse[]>(
    `/scenarios/${scenarioId}/simulated-employees`
  );
  return Array.isArray(list) ? list : [];
}

/**
 * Thêm mới nhân sự giả định vào kịch bản mô phỏng
 */
export async function createSimulatedEmployee(
  scenarioId: number,
  payload: CreateSimulatedEmployeePayload
): Promise<RecruitmentEvaluationResponse> {
  return await apiRequest<RecruitmentEvaluationResponse>(
    `/scenarios/${scenarioId}/simulated-employees`,
    {
      method: "POST",
      body: JSON.stringify(payload),
    }
  );
}

/**
 * Cập nhật thông tin nhân sự giả định
 */
export async function updateSimulatedEmployee(
  scenarioId: number,
  employeeId: number,
  payload: UpdateSimulatedEmployeePayload
): Promise<RecruitmentEvaluationResponse> {
  return await apiRequest<RecruitmentEvaluationResponse>(
    `/scenarios/${scenarioId}/simulated-employees/${employeeId}`,
    {
      method: "PUT",
      body: JSON.stringify(payload),
    }
  );
}

/**
 * Xóa nhân sự giả định khỏi kịch bản mô phỏng
 */
export async function deleteSimulatedEmployee(
  scenarioId: number,
  employeeId: number
): Promise<RecruitmentEvaluationResponse> {
  return await apiRequest<RecruitmentEvaluationResponse>(
    `/scenarios/${scenarioId}/simulated-employees/${employeeId}`,
    {
      method: "DELETE",
    }
  );
}
