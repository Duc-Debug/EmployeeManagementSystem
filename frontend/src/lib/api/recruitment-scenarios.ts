"use client";

import { apiRequest } from "../api-client";

export interface RoleEvaluationItem {
  projectRoleId: number;
  projectRoleCode: string;
  projectRoleName: string;
  originalShortfallHours: number;
  simulatedCapacityHours: number;
  remainingShortfallHours: number;
  suggestedRecruitsNeeded: number;
  isRoleBroken: boolean;
}

export interface RecruitmentEvaluationResponse {
  totalOriginalShortfallHours: number;
  totalSimulatedCapacityHours: number;
  totalRemainingShortfallHours: number;
  isPlanBroken: boolean;
  overloadedRoleCount: number;
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
): Promise<SimulatedEmployeeResponse> {
  return await apiRequest<SimulatedEmployeeResponse>(
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
): Promise<SimulatedEmployeeResponse> {
  return await apiRequest<SimulatedEmployeeResponse>(
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
): Promise<void> {
  await apiRequest<void>(
    `/scenarios/${scenarioId}/simulated-employees/${employeeId}`,
    {
      method: "DELETE",
    }
  );
}
