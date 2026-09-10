"use client";

import { apiRequest } from "../api-client";

export interface PendingEmployeeSkillItem {
  id: number;
  employeeId: number;
  employeeCode?: string;
  employeeName: string;
  orgUnitId?: number;
  orgUnitName?: string;
  skillId: number;
  skillCode?: string;
  skillName: string;
  skillCategory?: string;
  proficiencyLevel: number;
  yearsOfExperience: number;
  status: string;
  createdAt?: string;
}

export interface ApproveEmployeeSkillPayload {
  adjustedProficiencyLevel: number;
  reviewNotes?: string;
}

export interface ApprovedSkillResponse {
  id: number;
  employeeId: number;
  skillId: number;
  skillName: string;
  skillCode?: string;
  skillCategory?: string;
  proficiencyLevel: number;
  yearsOfExperience: number;
  status: string;
  approvedBy?: number;
  approvedAt?: string;
  reviewNotes?: string;
}

/**
 * Lấy danh sách các kỹ năng của nhân viên đang chờ quản lý xác nhận/phê duyệt
 */
export async function getPendingEmployeeSkills(
  keyword?: string
): Promise<PendingEmployeeSkillItem[]> {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
  return await apiRequest<PendingEmployeeSkillItem[]>(
    `/employee-skills/pending${query}`,
    {
      method: "GET",
    }
  );
}

/**
 * Xác nhận hoặc điều chỉnh mức thành thạo của kỹ năng kèm ghi chú đánh giá (NCL-02-CN-006)
 */
export async function approveEmployeeSkill(
  id: number,
  payload: ApproveEmployeeSkillPayload
): Promise<ApprovedSkillResponse> {
  return await apiRequest<ApprovedSkillResponse>(
    `/employee-skills/${id}/approve`,
    {
      method: "PUT",
      body: JSON.stringify(payload),
    }
  );
}
